package services;

import beans.request.TransferRequestBean;
import dao.AccountDao;
import exception.InvalidCurrencyTransfer;
import exception.InvalidTransferRequest;
import exception.NoAccountFoundException;
import models.Account;
import models.Account.CurrencyEnum;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.runners.MockitoJUnitRunner;
import play.db.jpa.JPAApi;
import services.impl.TransferServiceImpl;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransferServiceTest {

    private AccountDao accountDao;
    private TransferService underTest;
    private EntityManager em;
    private JPAApi jpaApi;

    @Before
    public void setup() {
        accountDao = mock(AccountDao.class);
        underTest = new TransferServiceImpl(accountDao);

        em = mock(EntityManager.class);
        jpaApi = mock(JPAApi.class);

        when(accountDao.jpaApi()).thenReturn(jpaApi);
        when(jpaApi.em()).thenReturn(em);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransfer_whenLogTransferThrowsException() {
        doThrow(IllegalArgumentException.class).when(em).persist(any());

        underTest.transfer(buildTransferRequest(2131234L, 213124L, 100, CurrencyEnum.SGD));
    }

    @Test(expected = NullPointerException.class)
    public void testTransfer_whenNullCurrency_thenLogTransferThrowsException() {
        doThrow(IllegalArgumentException.class).when(em).persist(any());

        underTest.transfer(buildTransferRequest(2131234L, 213124L, 100, null));
    }

    @Test(expected = InvalidTransferRequest.class)
    public void testTransfer_whenNullTransferAmount_thenTransferThrowsException() {
        TransferRequestBean request = TransferRequestBean.builder()
                .amount(null)
                .currency(CurrencyEnum.SGD)
                .fromAccountId(671628L)
                .toAccountId(872173L)
                .build();

        underTest.transfer(request);
    }

    @Test(expected = InvalidTransferRequest.class)
    public void testTransfer_whenNegativeTransferAmount_thenTransferThrowsException() {
        underTest.transfer(buildTransferRequest(82371683L, 413124L, -10, CurrencyEnum.USD));
    }

    @Test(expected = NoAccountFoundException.class)
    public void testTransfer_whenSenderAccountNotFound_andTransferThrowsException() {
        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.empty());

        underTest.transfer(buildTransferRequest(82371683L, 413124L, 10, CurrencyEnum.USD));
    }

    @Test(expected = NoAccountFoundException.class)
    public void testTransfer_whenReceiverAccountNotFound_andTransferThrowsException() {
        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(new Account()));
        when(accountDao.getAccount(eq(413124L))).thenReturn(Optional.empty());

        underTest.transfer(buildTransferRequest(82371683L, 413124L, 10, CurrencyEnum.USD));
    }

    @Test(expected = InvalidTransferRequest.class)
    public void testTransfer_whenSelfTransfer_andTransferThrowsException() {
        Account account = Account.builder().id(82371683L).build();

        when(accountDao.getAccount(eq(82371683L)))
                .thenReturn(Optional.of(account))
                .thenReturn(Optional.of(account));

        underTest.transfer(buildTransferRequest(82371683L, 82371683L, 10, CurrencyEnum.USD));
    }

    @Test(expected = InvalidCurrencyTransfer.class)
    public void testTransfer_whenAccountsHaveDifferentCurrency_andTransferThrowsException() {
        Account fromAccount = Account.builder().id(82371683L).currency(CurrencyEnum.SGD).build();
        Account toAccount = Account.builder().id(413124L).currency(CurrencyEnum.USD).build();

        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(fromAccount));
        when(accountDao.getAccount(eq(413124L))).thenReturn(Optional.of(toAccount));

        underTest.transfer(buildTransferRequest(82371683L, 413124L, 10, CurrencyEnum.USD));
    }

    @Test(expected = InvalidCurrencyTransfer.class)
    public void testTransfer_whenTransferCurrencyAndAccountCurrencyIsDifferent_andTransferThrowsException() {
        Account fromAccount = Account.builder().id(82371683L).currency(CurrencyEnum.SGD).build();
        Account toAccount = Account.builder().id(413124L).currency(CurrencyEnum.SGD).build();

        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(fromAccount));
        when(accountDao.getAccount(eq(413124L))).thenReturn(Optional.of(toAccount));

        underTest.transfer(buildTransferRequest(82371683L, 413124L, 10, CurrencyEnum.USD));
    }

    @Test
    public void testTransfer_whenInsufficientBalance_andTransferThrowsException() {
        Lock fromLock = mock(Lock.class);
        Lock toLock = mock(Lock.class);

        Account fromAccount = Account.builder().id(82371683L).currency(CurrencyEnum.SGD).balance(BigDecimal.TEN).lock(fromLock).build();
        Account toAccount = Account.builder().id(413124L).currency(CurrencyEnum.SGD).lock(toLock).build();

        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(fromAccount));
        when(accountDao.getAccount(eq(413124L))).thenReturn(Optional.of(toAccount));
        when(fromLock.tryLock()).thenReturn(true);
        when(toLock.tryLock()).thenReturn(true);

        try {
            underTest.transfer(buildTransferRequest(82371683L, 413124L, 11, CurrencyEnum.SGD));
        } catch (Exception e) {
            Assertions.assertThat(e.getMessage()).contains("The balance in the account not sufficient for this transfer");
        }

        // verify if lower account ID lock is taken first and released later
        InOrder inOrder = inOrder(toLock, fromLock);

        inOrder.verify(toLock).tryLock();
        inOrder.verify(fromLock).tryLock();

        inOrder.verify(fromLock).unlock();
        inOrder.verify(toLock).unlock();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTransfer_whenSufficientBalance_andTransferSuccess() {
        Lock fromLock = mock(Lock.class);
        Lock toLock = mock(Lock.class);

        Account fromAccount = Account.builder().id(82371683L).currency(CurrencyEnum.SGD).balance(BigDecimal.TEN).lock(fromLock).build();
        Account toAccount = Account.builder().id(82371684L).currency(CurrencyEnum.SGD).balance(BigDecimal.ONE).lock(toLock).build();

        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(fromAccount));
        when(accountDao.getAccount(eq(82371684L))).thenReturn(Optional.of(toAccount));
        when(fromLock.tryLock()).thenReturn(true);
        when(toLock.tryLock()).thenReturn(true);

        when(jpaApi.withTransaction(any(String.class), any(Boolean.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Function<EntityManager, Object> block = invocation.getArgumentAt(2, Function.class);
                    block.apply(em);
                    return null;
                });

        boolean result = underTest.transfer(buildTransferRequest(82371683L, 82371684L, 7, CurrencyEnum.SGD));

        InOrder inOrder = inOrder(toLock, fromLock);

        inOrder.verify(fromLock).tryLock();
        inOrder.verify(toLock).tryLock();

        inOrder.verify(toLock).unlock();
        inOrder.verify(fromLock).unlock();

        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.valueOf(3.0));
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.valueOf(8.0));
        assertThat(result).isTrue();
    }

    @Test
    public void testTransfer_whenLock1FailedToAcquire_andTransferFail() {
        Lock fromLock = mock(Lock.class);
        Lock toLock = mock(Lock.class);

        Account fromAccount = Account.builder().id(82371683L).currency(CurrencyEnum.SGD).balance(BigDecimal.TEN).lock(fromLock).build();
        Account toAccount = Account.builder().id(82371684L).currency(CurrencyEnum.SGD).balance(BigDecimal.ONE).lock(toLock).build();

        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(fromAccount));
        when(accountDao.getAccount(eq(82371684L))).thenReturn(Optional.of(toAccount));
        when(fromLock.tryLock()).thenReturn(false);

        boolean result = underTest.transfer(buildTransferRequest(82371683L, 82371684L, 7, CurrencyEnum.SGD));

        verify(fromLock).tryLock();
        verify(fromLock, never()).unlock();

        verify(toLock, never()).tryLock();
        verify(toLock, never()).unlock();

        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.TEN); // no change in balance
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.ONE);
        assertThat(result).isFalse();
    }

    @Test
    public void testTransfer_whenLock2FailedToAcquire_andTransferFail() {
        Lock fromLock = mock(Lock.class);
        Lock toLock = mock(Lock.class);

        Account fromAccount = Account.builder().id(82371683L).currency(CurrencyEnum.SGD).balance(BigDecimal.TEN).lock(fromLock).build();
        Account toAccount = Account.builder().id(82371684L).currency(CurrencyEnum.SGD).balance(BigDecimal.ONE).lock(toLock).build();

        when(accountDao.getAccount(eq(82371683L))).thenReturn(Optional.of(fromAccount));
        when(accountDao.getAccount(eq(82371684L))).thenReturn(Optional.of(toAccount));
        when(fromLock.tryLock()).thenReturn(true);
        when(toLock.tryLock()).thenReturn(false);

        boolean result = underTest.transfer(buildTransferRequest(82371683L, 82371684L, 7, CurrencyEnum.SGD));

        verify(fromLock).tryLock();
        verify(fromLock).unlock();

        verify(toLock).tryLock();
        verify(toLock, never()).unlock();

        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.TEN); // no change in balance
        assertThat(toAccount.getBalance()).isEqualTo(BigDecimal.ONE);
        assertThat(result).isFalse();
    }

    private TransferRequestBean buildTransferRequest(Long from, Long to, double amount, CurrencyEnum currency) {
        return TransferRequestBean.builder()
                .amount(BigDecimal.valueOf(amount))
                .currency(currency)
                .fromAccountId(from)
                .toAccountId(to)
                .build();
    }
}
