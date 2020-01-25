package dao.impl;

import com.google.inject.Inject;
import dao.CustomerDao;
import exception.NoAccountFoundException;
import lombok.extern.slf4j.Slf4j;
import models.Customer;
import models.TransferLog;
import org.springframework.util.CollectionUtils;
import play.db.jpa.JPAApi;

import javax.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class CustomerDaoImpl implements CustomerDao {

    private final JPAApi jpaApi;

    @Inject
    public CustomerDaoImpl(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    @Override
    public JPAApi jpaApi() {
        return this.jpaApi;
    }

    @Override
    public Optional<Customer> getCustomerById(Long customerId) {
        try {
            Customer customer = jpaApi.em().createQuery("select c from Customer c join Account a on c.id = a.customerId" +
                    " where c.id = :customerId", Customer.class)
                    .setParameter("customerId", customerId)
                    .getSingleResult();

            return Optional.ofNullable(customer);
        } catch (NoResultException e) {
            log.info("No customer found for id {}", customerId);
        }

        return Optional.empty();
    }

    @Override
    public void onboardCustomer(final Customer customer) {
        if (CollectionUtils.isEmpty(customer.getAccounts()))
            throw new NoAccountFoundException("Trying to create customer with no account information");

        jpaApi.em().persist(customer);
    }

    @Override
    public List<TransferLog> getTransferLogs(Long customerId, Long accountId) {
        return jpaApi.em().createQuery("select tl from TransferLog tl" +
                " join Account a on tl.fromAccountId = a.id or tl.toAccountId = a.id" +
                " join Customer c on c.id = a.customerId where a.id = :accountId and c.id = :customerId", TransferLog.class)
                .setParameter("accountId", accountId)
                .setParameter("customerId", customerId)
                .getResultList();
    }
}
