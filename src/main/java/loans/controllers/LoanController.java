package loans.controllers;

import javax.validation.Valid;

import loans.entities.Loan;
import loans.entities.MonthyPlan;
import loans.repositories.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Controller
public class LoanController {

    private final LoanRepository loanRepository;

    @Autowired
    public LoanController(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @GetMapping("/index")
    public String showUserList(Model model) {
        model.addAttribute("loans", loanRepository.findAll());
        return "index";
    }

    @GetMapping("/signup")
    public String showSignUpForm(Loan loan) {
        return "create-loan";
    }

    @PostMapping("/addloan")
    public String addUser(@Valid Loan loan, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "add-user";
        }
        loan.setPayment((int)(100*loan.getAmount() * loan.getAip()/100/12 * Math.pow(1 + loan.getAip()/100/12, loan.getNom())/(Math.pow(1 + loan.getAip()/100/12, loan.getNom())-1))/100.0);
        loanRepository.save(loan);
        return "redirect:/index";
    }

    @GetMapping("/edit/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        List<MonthyPlan> plan = calculateLoanPlan(loan);
        model.addAttribute("plan", plan);
        return "loan-details";
    }

    private List<MonthyPlan> calculateLoanPlan(Loan loan) {
        BigDecimal amount = BigDecimal.valueOf(loan.getAmount());
        BigDecimal aip = BigDecimal.valueOf(loan.getAip()/100/12);
        int nom = loan.getNom();
        BigDecimal remainingBalance = amount;
        BigDecimal interestAmount;
        BigDecimal payment = BigDecimal.valueOf(loan.getPayment());
        amount = amount.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        remainingBalance = remainingBalance.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        List<MonthyPlan> plan = new ArrayList<>();
        for(int i = 1; i <= nom; i++) {
            interestAmount = remainingBalance.multiply(aip);
            System.out.println(interestAmount);
            interestAmount = interestAmount.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            System.out.println(interestAmount);
            remainingBalance = remainingBalance.subtract(payment.subtract(interestAmount));
            remainingBalance = remainingBalance.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            if(i== nom)
                plan.add(new MonthyPlan(i,payment.add(remainingBalance),payment.subtract(interestAmount).add(remainingBalance),interestAmount,BigDecimal.valueOf(0.0)));
            else
                plan.add(new MonthyPlan(i,payment,payment.subtract(interestAmount),interestAmount, remainingBalance));
        }
        return  plan;
    }
}
