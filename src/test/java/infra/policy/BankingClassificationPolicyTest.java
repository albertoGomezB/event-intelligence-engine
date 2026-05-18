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
    void shouldAllowSupportedBankingCategory() {
        assertThat(policy.isCategoryAllowed("PAYMENTS")).isTrue();
    }

    @Test
    void shouldRejectUnsupportedCategory() {
        assertThat(policy.isCategoryAllowed("CRYPTO")).isFalse();
    }

    @Test
    void shouldRejectNullCategory() {
        assertThat(policy.isCategoryAllowed(null)).isFalse();
    }
}
