package dao.impl;

import com.google.inject.Inject;
import dao.AccountDao;
import lombok.extern.slf4j.Slf4j;
import models.Account;
import play.db.jpa.JPAApi;

import javax.persistence.NoResultException;
import java.util.Optional;

@Slf4j
public class AccountDaoImpl implements AccountDao {
    private final JPAApi jpaApi;

    @Inject
    public AccountDaoImpl(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    @Override
    public JPAApi jpaApi() {
        return this.jpaApi;
    }

    @Override
    public Optional<Account> getAccount(Long accountId) {
        try {
            Account account = jpaApi.em().createQuery("select a from Account a where a.id = :accountId", Account.class)
                    .setParameter("accountId", accountId)
                    .getSingleResult();
            return Optional.ofNullable(account);
        } catch (NoResultException e) {
            log.error("No account found with id {}", accountId);
        }

        return Optional.empty();
    }
}
