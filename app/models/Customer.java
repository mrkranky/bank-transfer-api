package models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="customers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class Customer extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Account> accounts = new ArrayList<>();

    @JsonIgnore
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name="deleted_at")
    private Date deletedAt;

    private void addAccount(Account account) {
        account.setCustomer(this);
        accounts.add(account);
    }

    public static class CustomerBuilder {
        private String firstName;
        private String lastName;
        private List<Account> accounts = new ArrayList<>();

        private CustomerBuilder() {
        }

        public static CustomerBuilder builder() {
            return new CustomerBuilder();
        }

        public CustomerBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public CustomerBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public CustomerBuilder withAccounts(Account... accounts) {
            this.accounts.addAll(Arrays.asList(accounts));
            return this;
        }

        public CustomerBuilder withAccounts(List<Account> accounts) {
            this.accounts.addAll(accounts);
            return this;
        }

        public Customer build() {
            Customer customer = new Customer();

            customer.setFirstName(firstName);
            customer.setLastName(lastName);
            accounts.forEach(customer::addAccount);

            return customer;
        }
    }
}