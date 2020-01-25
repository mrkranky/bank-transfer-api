package dao;

import com.google.inject.ImplementedBy;
import dao.impl.CustomerDaoImpl;
import models.Customer;
import models.TransferLog;
import play.db.jpa.JPAApi;

import java.util.List;
import java.util.Optional;

@ImplementedBy(CustomerDaoImpl.class)
public interface CustomerDao {

    JPAApi jpaApi();

    Optional<Customer> getCustomerById(Long customerId);

    void onboardCustomer(Customer customer);

    List<TransferLog> getTransferLogs(Long customerId, Long accountId);
}
