package com.colvir.shared.ui.mainScreen

import com.colvir.mbul.MR
import com.colvir.shared.constructor.LayoutIDs
import com.colvir.shared.repositories.AccountsRepository
import com.colvir.shared.repositories.DocStatisticRepository
import com.colvir.shared.repositories.UserClientRepository
import com.colvir.shared.services.UserPreferences
import com.colvir.shared.DTOs.products.account.AccountDTO
import com.colvir.shared.constructor.IconIDs
import com.colvir.shared.ui.AbstractViewModel
import constructor.CommonExpandableSection
import constructor.CommonRowModel
import constructor.SwipeGesture
import constructor.uiModels.AppBarControl
import constructor.uiModels.HorizontalCollectionControl
import constructor.uiModels.ListControl
import infrastructure.network.NetworkRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import ui.auxiliary.ProgressMode

class MainScreenViewModel(
        private val userClientRepository: UserClientRepository,
        private val accountsRepository: AccountsRepository,
        private val userPreferences: UserPreferences,
        private val docStatisticRepository: DocStatisticRepository
) : AbstractViewModel() {

    val appBarControl = AppBarControl().apply {
        titleLive.value = userPreferences.userName
    }

    override var delayBeetwenUpdatesInSeconds: Int = 1800

    val mainMenuTree = ListControl("mainMenu",
            addSwipeToRefresh = false,
            expandSectionWhenTapOnSection = true,
            showLeftIcon = true,
            addDivider = false
    )
    val ads = HorizontalCollectionControl(name = "ads", onSelectItemAction = ::onSelectCard)

    lateinit var accountsSection: CommonRowModel
    lateinit var accountsInListLoader: InListLoader<AccountDTO>

    init {
        compositeDisposable += userClientRepository.updateLive.observeDisposable {
            appBarControl.secondTitleLive.value = userClientRepository.item?.data?.firstOrNull()?.name
        }
        compositeDisposable += userPreferences.doNotShowTheseAccounts.subscribe {
            createHierarchyList()
        }
    }

    override fun initializeComponents() {
        super.initializeComponents()

        // Accounts in list loading
        accountsSection = CommonRowModel(
                uniqueId = "grp_acnt",
                layoutId = LayoutIDs.EXPANDABLE_GROUP,
                iconId = IconIDs.icAccount,
                title = getString(MR.strings.mains_list_lists_accounts),
                onTapAction = ::openAccountsList)

        accountsInListLoader = InListLoader(
                rootSection = accountsSection,
                repository = accountsRepository,
                rebuildHierarchyTree = ::createHierarchyList,
                noItemsMessage = getString(MR.strings.acclst_no_accounts),
                viewModel = this,
                openFullListAction = {
                    navigation.mainContainerToAccountsList()
                },
                getCollection = {
                    val excludeList = userPreferences.doNotShowTheseAccounts.value
                            ?: emptyList<String>()
                    val swipeGesture = listOf(
                            SwipeGesture(
                                    side = SwipeGesture.Side.RIGHT,
                                    color = MR.colors.orange.color,
                                    iconId = IconIDs.icVisibilityOff,
                                    iconTintColor = MR.colors.white.color,
                                    action = ::hideAccount,
                                    layoutId = LayoutIDs.SwipeLayouts.MAIN_SWIPE_LAYOUT
                            )
                    )
                    accountsRepository.items
                            .filter {
                                !excludeList.contains(it.id)
                            }
                            .map { accountDto ->
                                val row = accountDto.createCommonRowModel(this)
                                row.uniqueId = "${accountsSection.uniqueId}_${accountDto.id}"
                                row.layoutId = LayoutIDs.MainScreen.ACCOUNT_ROW
                                row.titleColor = MR.colors.colorPrimaryLight.color
                                row.onTapAction = ::openAccountDetails
                                row.data = accountDto
                                row.swipeGestures = swipeGesture
                                row
                            }
                })

        compositeDisposable += accountsRepository.isUpdatingLive.observeDisposable(skipFirstCall = true) {
            createHierarchyList()
        }

        createHierarchyList()
        //

        // ADS
        ads.hierarchyTreeLive.value = CommonRowModel("root").apply {
            children = mainMenuTree.hierarchyTreeLive.value?.children?.map {
                val row = it as CommonRowModel
                CommonRowModel(
                        uniqueId = row.uniqueId,
                        title = row.title,
                        iconId = row.iconId,
                        layoutId = LayoutIDs.CARD_1
                )
            } ?: emptyList()
        }
    }

    private fun onSelectCard(item: CommonExpandableSection) {
        val itemInMainTree = mainMenuTree.hierarchyTreeLive.value?.children?.firstOrNull { it.uniqueId == item.uniqueId }
        if (itemInMainTree != null) {
            itemInMainTree.onTapAction?.invoke(itemInMainTree)
        }
    }

    private fun openAccountDetails(obj: Any?) {
        val accountDto = obj as AccountDTO
        navigation.mainContainerToAccountFullDetails(com.colvir.shared.ui.accounts.AccountFullDetailsViewModel.Args(accountId = accountDto.id))
    }

    private fun openAccountsList(obj: Any?) {
        navigation.mainContainerToAccountsList()
    }

    private fun hideAccount(row: CommonRowModel) {
        val accountDTO = row.data as? AccountDTO
        if (accountDTO != null) {
            userPreferences.doNotShowTheseAccounts.addValue(accountDTO.id)
        }
    }

    override fun updateDataInternal(pullToRefresh: Boolean) {
        super.updateDataInternal(pullToRefresh)

        NetworkRequest(
                request = {
                    val deferreds = listOf(
                            viewModelScope.async { userClientRepository.updateAsync(false) },
                            viewModelScope.async { docStatisticRepository.updateAsync(false) }
                            //viewModelScope.async { accountsRepository.updateAsync(false) }
                    )
                    val allResponses = deferreds.awaitAll()
                    if (allResponses.any { !it.isSuccessful }) {
                        allResponses.first { !it.isSuccessful }
                    } else {
                        allResponses[0]
                    }
                },
                viewModel = this,
                showProgress = if (accountsRepository.size() == 0) ProgressMode.SHOW else ProgressMode.SHOW_BACKGROUND)
                .launch()

    }

    private fun createHierarchyList() {

        val rootList = mutableListOf<CommonRowModel>()

        // Accounts
        rootList.add(accountsSection)
        accountsInListLoader.build()

        // Deposits
        rootList.add(CommonRowModel(
                uniqueId = "deposits",
                layoutId = LayoutIDs.GROUP,
                title = getString(MR.strings.mains_list_lists_depo),
                iconId = IconIDs.icDeposit,
                onTapAction = {
                    navigation.mainContainerToDepositsList()
                })
        )

        // Loans
        rootList.add(CommonRowModel(
                uniqueId = "loans",
                layoutId = LayoutIDs.GROUP,
                title = getString(MR.strings.mains_list_lists_loans),
                iconId = IconIDs.icCredit,
                onTapAction = {
                    navigation.mainContainerToCreditsList()
                })
        )

        // Corp cards
        rootList += CommonRowModel(
                uniqueId = "grp_copr_cards",
                layoutId = LayoutIDs.GROUP,
                title = getString(MR.strings.mains_list_corpcard),
                iconId = IconIDs.icPayment,
                onTapAction = {
                    navigation.mainContainerToCorpCardsList()
                })

        val root = CommonRowModel(uniqueId = "root").apply {
            children = rootList
        }

        mainMenuTree.hierarchyTreeLive.value = root

    }

}

