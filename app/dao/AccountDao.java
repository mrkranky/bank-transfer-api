package dao;

import com.google.inject.ImplementedBy;
import dao.impl.AccountDaoImpl;
import models.Account;
import play.db.jpa.JPAApi;

import java.util.Optional;

@ImplementedBy(AccountDaoImpl.class)
public interface AccountDao {

    JPAApi jpaApi();

    Optional<Account> getAccount(Long accountId);
}
