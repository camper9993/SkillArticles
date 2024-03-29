package ru.skillbranch.skillarticles.ui.articles

import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.view.Menu
import android.view.MenuItem
import android.widget.AutoCompleteTextView
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_articles.*
import kotlinx.android.synthetic.main.search_view_layout.view.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesState
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class ArticlesFragment : BaseFragment<ArticlesViewModel>() {
    override val viewModel: ArticlesViewModel by activityViewModels()
    override val layout: Int = R.layout.fragment_articles
    override val binding: ArticlesBinding by lazy { ArticlesBinding() }
    private val args: ArticlesFragmentArgs by navArgs()
    private lateinit var suggestionsAdapter: SimpleCursorAdapter

    override val prepareToolbar: (ToolbarBuilder.() -> Unit) = {
        addMenuItem(
            MenuItemHolder(
                "Search",
                R.id.action_search,
                R.drawable.ic_search_black_24dp,
                R.layout.search_view_layout
            )
        )
        addMenuItem(
            MenuItemHolder(
                "Filter",
                R.id.action_search,
                R.drawable.ic_baseline_filter_list_24,
                null
            ){menuItem ->
                val action = ArticlesFragmentDirections.choseCategory(
                    binding.selectedCategories.toTypedArray(),
                    binding.categories.toTypedArray()
                )
                viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
            }
        )
    }

    private val articlesAdapter = ArticlesAdapter { item, isToggleBookmark ->

        if (isToggleBookmark) {
            viewModel.handleToggleBookmark(item.id)
        } else {
            val action = ArticlesFragmentDirections.actionToPageArticle(
                item.id,
                item.author,
                item.authorAvatar!!,
                item.category,
                item.categoryIcon,
                item.poster,
                item.title,
                item.date
            )
            viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        suggestionsAdapter = SimpleCursorAdapter(
            context,
            android.R.layout.simple_list_item_1,
            null,
            arrayOf("tag"),
            intArrayOf(android.R.id.text1),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
        suggestionsAdapter.setFilterQueryProvider { constraint -> populateAdapter(constraint) }
        setHasOptionsMenu(true)
    }

    private fun populateAdapter(constraint: CharSequence?) : Cursor {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "tag"))
        constraint ?: return cursor

        val currentCursor = suggestionsAdapter.cursor
        currentCursor.moveToFirst()
        for (i in 0 until currentCursor.count) {
            val tagValue = currentCursor.getString(1)
            if(tagValue.contains(constraint, true)) cursor.addRow(arrayOf<Any>(i, tagValue))
            currentCursor.moveToNext()
        }
        return cursor
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.action_search)
        val searchView = menuItem.actionView as SearchView
        if (binding.isSearch) {
            menuItem.expandActionView()
            searchView.setQuery(binding.searchQuery, false)
        }

        val autoTv = searchView.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        autoTv.threshold = 1

        searchView.suggestionsAdapter = suggestionsAdapter
        searchView.setOnSuggestionListener(object: SearchView.OnSuggestionListener{
            override fun onSuggestionSelect(position: Int): Boolean = false

            override fun onSuggestionClick(position: Int): Boolean {
                suggestionsAdapter.cursor.moveToPosition(position)
                val query = suggestionsAdapter.cursor.getString(1)
                searchView.setQuery(query, true)
                viewModel.handleSuggestion(query)
                return false
            }

        })

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }

        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })
    }

    override fun onDestroyView() {
        toolbar.search_view?.setOnQueryTextListener(null)
        super.onDestroyView()
    }


    override fun setupViews() {
        with(rv_articles) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        viewModel.observeList(viewLifecycleOwner, args.isBookmarks) {
            articlesAdapter.submitList(it)
        }

        viewModel.observeTags(viewLifecycleOwner) {
            binding.tags = it
        }

        viewModel.observeCategories(viewLifecycleOwner) {
            binding.categories = it
        }
    }

    inner class ArticlesBinding : Binding() {
        var categories: List<CategoryData> = emptyList()
        var selectedCategories: List<String> by RenderProp(emptyList()) {

        }
        var searchQuery: String? = null
        var isSearch: Boolean = false
        var isLoading: Boolean by RenderProp(true) {
            //TODO show shimmer on rv_list
        }

        var isHashtagSearch : Boolean by RenderProp(false)
        var tags : List<String> by RenderProp(emptyList())

        override fun bind(data: IViewModelState) {
            data as ArticlesState
            isSearch = data.isSearch
            searchQuery = data.searchQuery
            isLoading = data.isLoading
            isHashtagSearch = data.isHashTagSearch
            selectedCategories = data.selectedCategories
        }

        override val afterInflated: (() -> Unit)? = {
            dependsOn<Boolean, List<String>>(::isHashtagSearch, ::tags) { ihs, tags ->
                val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "tag"))
                if (ihs && tags.isEmpty()) {
                    for ((counter, tag) in tags.withIndex()) {
                        cursor.addRow(arrayOf<Any>(counter, tag))
                    }
                }
                suggestionsAdapter.changeCursor(cursor)
            }
        }
    }

}
