package com.abistama.supporthero.domain.slack

data class SlackEnterpriseId internal constructor(
    val enterpriseId: String?,
) {
    init {
        enterpriseId?.let { validateEnterpriseId(it) }
    }

    override fun toString(): String = enterpriseId ?: NO_ENTERPRISE

    private fun validateEnterpriseId(enterpriseId: String) {
        if (!"^(E)[a-zA-Z\\d]{10}".toRegex().matches(enterpriseId)) {
            throw SlackException("Slack Enterprise Id isn't valid")
        }
    }

    companion object {
        private const val NO_ENTERPRISE = "NO_ENTERPRISE"
    }
}
