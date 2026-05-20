package infra.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankingClassificationPolicyTest {

    private final BankingClassificationPolicy policy = new BankingClassificationPolicy();

    @Test
    void shouldRequireHumanReviewWhenConfidenceIsBelowThreshold() {
        assertThat(policy.requiresHumanReview(0.79)).isTrue();
    }

    @Test
    void shouldNotRequireHumanReviewWhenConfidenceIsEqualToThreshold() {
        assertThat(policy.requiresHumanReview(0.80)).isFalse();
    }

    @Test
    void shouldNotRequireHumanReviewWhenConfidenceIsAboveThreshold() {
        assertThat(policy.requiresHumanReview(0.92)).isFalse();
    }

    @Test
    void shouldAllowSupportedBankingClassification() {
        assertThat(policy.isClassificationAllowed("PAYMENTS", "DIRECT_DEBIT")).isTrue();
    }

    @Test
    void shouldRejectUnsupportedSubcategoryForCategory() {
        assertThat(policy.isClassificationAllowed("UNKNOWN_CATEGORY", "DIRECT_DEBIT")).isFalse();
    }

    @Test
    void shouldRejectNullCategory() {
        assertThat(policy.isClassificationAllowed(null, null)).isFalse();
    }

    @Test
    void shouldRejectNullSubcategory() {
        assertThat(policy.isClassificationAllowed("PAYMENTS", null)).isFalse();
    }
}
