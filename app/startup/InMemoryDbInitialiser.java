package startup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dao.CustomerDao;
import models.Account;
import models.Customer;
import play.Logger;

import java.math.BigDecimal;

import static models.Customer.CustomerBuilder;
import static models.Account.CurrencyEnum;

@Singleton
public class InMemoryDbInitialiser {
    private final CustomerDao customerDao;

    @Inject
    public InMemoryDbInitialiser(CustomerDao customerDao) {
        this.customerDao = customerDao;
        init();
    }

    public void init() {
        Customer c1 = CustomerBuilder.builder().withFirstName("Christopher").withLastName("Williams")
                .withAccounts(Account.builder().balance(BigDecimal.valueOf(10000)).currency(CurrencyEnum.SGD).build()).build();

        Customer c2 = CustomerBuilder.builder().withFirstName("Joseph").withLastName("Taylor")
                .withAccounts(Account.builder().balance(BigDecimal.valueOf(20000)).currency(CurrencyEnum.USD).build()).build();

        Customer c3 = CustomerBuilder.builder().withFirstName("Daniel").withLastName("Brown")
                .withAccounts(Account.builder().balance(BigDecimal.valueOf(30000)).currency(CurrencyEnum.EUR).build()).build();

        // two accounts for same customer with different currencies
        Customer c4 = CustomerBuilder.builder().withFirstName("Joshua").withLastName("Johnson")
                .withAccounts(
                        Account.builder().balance(BigDecimal.valueOf(40000)).currency(CurrencyEnum.USD).build(),
                        Account.builder().balance(BigDecimal.valueOf(22000)).currency(CurrencyEnum.SGD).build()
                ).build();

        // two accounts for same customer with same currencies
        Customer c5 = CustomerBuilder.builder().withFirstName("Matthew").withLastName("Miller")
                .withAccounts(
                        Account.builder().balance(BigDecimal.valueOf(50000)).currency(CurrencyEnum.SGD).build(),
                        Account.builder().balance(BigDecimal.valueOf(67000)).currency(CurrencyEnum.SGD).build()
                ).build();

        customerDao.jpaApi().withTransaction(() -> {
            customerDao.onboardCustomer(c1);
            customerDao.onboardCustomer(c2);
            customerDao.onboardCustomer(c3);
            customerDao.onboardCustomer(c4);
            customerDao.onboardCustomer(c5);

            return null;
        });

        Logger.info("\nIN-MEMORY ACCOUNTS TABLE - \nID  \t      BALANCE  \tCURRENCY  \tCUSTOMER_ID\n" +
                "19283746\t  10000.00\t   SGD\t        1\n" +
                "19283747\t  20000.00\t   USD\t        2\n" +
                "19283748\t  30000.00\t   EUR\t        3\n" +
                "19283749\t  40000.00\t   USD\t        4\n" +
                "19283750\t  22000.00\t   SGD\t        4\n" +
                "19283751\t  50000.00\t   SGD\t        5\n" +
                "19283752\t  67000.00\t   SGD\t        5");
    }
}
