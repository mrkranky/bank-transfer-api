package dao;

import dao.impl.CustomerDaoImpl;
import exception.NoAccountFoundException;
import models.Account;
import models.Customer;
import models.TransferLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import play.db.jpa.JPAApi;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomerDaoTest {

    private JPAApi jpaApi;
    private EntityManager em;
    private TypedQuery typedQuery;
    private CustomerDao customerDao;

    @Before
    public void setup() {
        jpaApi = mock(JPAApi.class);
        em = mock(EntityManager.class);
        typedQuery = mock(TypedQuery.class);

        customerDao = new CustomerDaoImpl(jpaApi);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetCustomerById_whenCustomerNotFound() {
        when(jpaApi.em()).thenReturn(em);
        when(em.createQuery(any(), any())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyObject())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);

        Optional<Customer> result = customerDao.getCustomerById(23234L);
        assertThat(result.isPresent()).isFalse();

        verify(em).createQuery(eq("select c from Customer c join Account a on c.id = a.customerId where c.id = :customerId"), any());
        verify(typedQuery).setParameter(eq("customerId"), eq(23234L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetCustomerById_whenCustomerFound() {
        Customer customer = Customer.CustomerBuilder.builder().withFirstName("Bryan").withLastName("Adams").build();
        customer.setId(23234L);

        when(jpaApi.em()).thenReturn(em);
        when(em.createQuery(any(), any())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(customer);

        Optional<Customer> result = customerDao.getCustomerById(23234L);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(23234L);
        assertThat(result.get().getFirstName()).isEqualTo("Bryan");
        assertThat(result.get().getLastName()).isEqualTo("Adams");

        verify(typedQuery).setParameter(anyString(), eq(23234L));
    }

    @Test(expected = NoAccountFoundException.class)
    public void testCustomerOnboarding_whenCustomerAccountsIsNotSet() {
        Customer customer = Customer.CustomerBuilder.builder().withFirstName("Bryan").withLastName("Adams").build();
        customerDao.onboardCustomer(customer);
    }

    @Test(expected = NoAccountFoundException.class)
    public void testCustomerOnboarding_whenCustomerHasZeroAccounts() {
        Customer customer = Customer.CustomerBuilder.builder().withFirstName("Bryan").withLastName("Adams")
                .withAccounts(Collections.emptyList()).build();
        customerDao.onboardCustomer(customer);
    }

    @Test
    public void testCustomerOnboarding_whenValidCustomer() {
        Account account = Account.builder().id(21321L).currency(Account.CurrencyEnum.SGD).balance(BigDecimal.TEN).build();
        Customer customer = Customer.CustomerBuilder.builder().withFirstName("Bryan").withLastName("Adams")
                .withAccounts(Collections.singletonList(account)).build();

        when(jpaApi.em()).thenReturn(em);

        customerDao.onboardCustomer(customer);

        verify(em).persist(eq(customer));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTransferLogs() {
        TransferLog transferLog = TransferLog.builder().fromAccountId(93848984L).toAccountId(93848985L)
                .amount(BigDecimal.TEN).status(TransferLog.Status.COMPLETED).build();

        when(jpaApi.em()).thenReturn(em);
        when(em.createQuery(any(), any())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(transferLog));

        List<TransferLog> result = customerDao.getTransferLogs(23123L, 93848984L);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(result.get(0).getFromAccountId()).isEqualTo(93848984L);
        assertThat(result.get(0).getToAccountId()).isEqualTo(93848985L);
        assertThat(result.get(0).getStatus()).isEqualByComparingTo(TransferLog.Status.COMPLETED);

        verify(em).createQuery(eq("select tl from TransferLog tl join Account a on tl.fromAccountId = a.id or tl.toAccountId = a.id " +
                "join Customer c on c.id = a.customerId where a.id = :accountId and c.id = :customerId"), any());
        verify(typedQuery).setParameter(eq("accountId"), eq(93848984L));
        verify(typedQuery).setParameter(eq("customerId"), eq(23123L));
    }

    @Test
    public void testGetJPAApi() {
        JPAApi result = customerDao.jpaApi();
        assertThat(result).isEqualTo(jpaApi);
    }
}
