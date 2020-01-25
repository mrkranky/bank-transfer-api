package services.impl;

import beans.request.TransferRequestBean;
import com.google.inject.Inject;
import dao.AccountDao;
import exception.InsufficientBalance;
import exception.InvalidCurrencyTransfer;
import exception.InvalidTransferRequest;
import exception.NoAccountFoundException;
import models.Account;
import models.Account.CurrencyEnum;
import models.TransferLog;
import org.apache.commons.lang3.tuple.Pair;
import play.Logger;
import services.TransferService;

import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;

public class TransferServiceImpl implements TransferService {
    private final AccountDao accountDao;

    @Inject
    public TransferServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public boolean transfer(TransferRequestBean transferRequestBean) {
        TransferLog transferLog = null;

        try {
            transferLog = logTransfer(transferRequestBean);

            if (transferRequestBean.getAmount() == null || transferRequestBean.getAmount().compareTo(BigDecimal.ZERO) <= 0)
                throw new InvalidTransferRequest("Invalid amount to transfer");

            Pair<Account, Account> accounts =
                    getAccountDetails(transferRequestBean.getFromAccountId(), transferRequestBean.getToAccountId());
            Account fromAccount = accounts.getLeft();
            Account toAccount = accounts.getRight();

            validateTransfer(fromAccount, toAccount, transferRequestBean.getCurrency());
            boolean isTransferSuccess = doAccountTransfer(fromAccount, toAccount, transferRequestBean);

            if (isTransferSuccess)
                transferLog.setStatus(TransferLog.Status.COMPLETED);
            else
                transferLog.setStatus(TransferLog.Status.FAILED);

            return isTransferSuccess;
        } catch (Exception e) {
            if (transferLog != null) {
                transferLog.setStatus(TransferLog.Status.FAILED);
            }
            throw e;
        }
    }

    private Pair<Account, Account> getAccountDetails(Long fromAccountId, Long toAccountId) {
        synchronized (this) {
            Account fromAccount = accountDao.getAccount(fromAccountId)
                    .orElseThrow(() -> new NoAccountFoundException("Account number not found = " + fromAccountId));
            Account toAccount = accountDao.getAccount(toAccountId)
                    .orElseThrow(() -> new NoAccountFoundException("Account number not found = " + toAccountId));

            return Pair.of(fromAccount, toAccount);
        }
    }

    private void validateTransfer(Account from, Account to, CurrencyEnum transferCurrency) {
        if (from.getId().equals(to.getId()))
            throw new InvalidTransferRequest("Cannot transfer funds within the same bank account = " + from.getId());

        if (from.getCurrency() != to.getCurrency())
            throw new InvalidCurrencyTransfer("Given accounts have different currencies of " + from.getCurrency() + " and " + to.getCurrency());

        if (from.getCurrency() != transferCurrency)
            throw new InvalidCurrencyTransfer("Transfer currency " + transferCurrency + " and account currrency " + from.getCurrency() + " are different");
    }

    private TransferLog logTransfer(TransferRequestBean transferRequestBean) {
        TransferLog transferLog = TransferLog.builder()
                .fromAccountId(transferRequestBean.getFromAccountId())
                .toAccountId(transferRequestBean.getToAccountId())
                .amount(transferRequestBean.getAmount())
                .requestedCurrency(transferRequestBean.getCurrency().toString())
                .status(TransferLog.Status.PENDING).build();

        accountDao.jpaApi().em().persist(transferLog);
        return transferLog;
    }

    private boolean doAccountTransfer(Account from, Account to, TransferRequestBean transferRequestBean) {
        // acquire lock always in the same order to avoid deadlock
        final Lock lock1 = from.getId() < to.getId() ? from.getLock() : to.getLock();
        final Lock lock2 = from.getId() < to.getId() ? to.getLock() : from.getLock();

        if (lock1.tryLock()) {
            try {
                if (lock2.tryLock()) {
                    try {
                        final BigDecimal transferAmount = transferRequestBean.getAmount();

                        if (!hasSufficientBalance(from, transferAmount))
                            throw new InsufficientBalance("The balance in the account not sufficient for this transfer");
                        Logger.info("Transferring funds={}, between account={} to account={}", transferAmount, from.getId(), to.getId());

                        accountDao.jpaApi().withTransaction("default", false, em -> {
                            from.setBalance(from.getBalance().subtract(transferAmount));
                            to.setBalance(to.getBalance().add(transferAmount));

                            em.merge(from);
                            em.merge(to);
                            return null;
                        });

                        return true;
                    } finally {
                        lock2.unlock();
                    }
                }
            } finally {
                lock1.unlock();
            }
        }

        return false;
    }

    private boolean hasSufficientBalance(Account from, BigDecimal transferAmount) {
        return transferAmount.compareTo(from.getBalance()) <= 0;
    }
}
