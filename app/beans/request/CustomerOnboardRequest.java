package beans.request;

import exception.InvalidOnboardRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.Account;
import models.Customer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOnboardRequest implements Serializable {

    private static final long serialVersionUID = 8330365563423885977L;

    private String firstName;
    private String lastName;
    private List<Account> accounts;

    public Customer buildRequest() {
        validate();

        return Customer.CustomerBuilder.builder().withFirstName(firstName).withLastName(lastName)
                .withAccounts(accounts).build();
    }

    private void validate() {
        if (StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName))
            throw new InvalidOnboardRequest("first name or last name cannot be empty");

        if (CollectionUtils.isEmpty(accounts))
            throw new InvalidOnboardRequest("No accounts found to onboard customer");

        accounts.forEach(account -> {
            if (account.getBalance().compareTo(BigDecimal.ZERO) < 0)
                throw new InvalidOnboardRequest("Account with negative currency cannot be onboarded");
        });
    }
}
