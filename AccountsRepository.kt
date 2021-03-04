package com.colvir.shared.repositories

import repositories.AbstractNetworkListRepository
import com.colvir.shared.APIs.MainApi
import com.colvir.shared.DTOs.products.account.AccountDTO
import com.colvir.shared.DTOs.products.account.AccountListDTO
import infrastructure.network.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountsRepository(
        private val mainApi: MainApi
) : AbstractNetworkListRepository<AccountDTO, AccountListDTO>(tresholdIsSec = 600) {

    fun accountsWithAllowedTransactionHistory(): List<AccountDTO> {
        return items.filter { it.walLock?.code == "0" }
    }

    override suspend fun updateInternal(forcedUpdate: Boolean): NetworkResponse<AccountListDTO> {

        val response = mainApi.accountList()
        if (!response.isSuccessful) {
            return response
        }

        items = (response.body?.dataSet ?: emptyList()).toMutableList()
        withContext(Dispatchers.Main) {
            structureChangesWatcher.fireOnStructureChanged()
        }

        return response
    }
}