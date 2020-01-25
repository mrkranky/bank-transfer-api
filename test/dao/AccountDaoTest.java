package dao;

import dao.impl.AccountDaoImpl;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import play.db.jpa.JPAApi;
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
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AccountDaoTest {
    private JPAApi jpaApi;
    private EntityManager em;
    private TypedQuery typedQuery;
    private AccountDao accountDao;

    @Before
    public void setup() {
        jpaApi = mock(JPAApi.class);
        em = mock(EntityManager.class);
        typedQuery = mock(TypedQuery.class);

        accountDao = new AccountDaoImpl(jpaApi);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAccount_whenAccountNotFound() {
        when(jpaApi.em()).thenReturn(em);
        when(em.createQuery(any(), any())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyObject())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);

        Optional<Account> result = accountDao.getAccount(21321L);
        assertThat(result.isPresent()).isFalse();

        verify(em).createQuery(eq("select a from Account a where a.id = :accountId"), any());
        verify(typedQuery).setParameter(eq("accountId"), eq(21321L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAccount_whenAccountFound() {
        Account account = Account.builder().id(21321L).currency(CurrencyEnum.SGD).balance(BigDecimal.TEN).build();

        when(jpaApi.em()).thenReturn(em);
        when(em.createQuery(any(), any())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyObject())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(account);

        Optional<Account> result = accountDao.getAccount(21321L);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(21321L);
        assertThat(result.get().getBalance()).isEqualTo(BigDecimal.TEN);
        assertThat(result.get().getCurrency()).isEqualTo(CurrencyEnum.SGD);
    }

    @Test
    public void testGetJPAApi() {
        JPAApi result = accountDao.jpaApi();
        assertThat(result).isEqualTo(jpaApi);
    }
}
