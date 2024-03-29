package ru.skillbranch.skillarticles.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel

class ChoseCategoryDialog : DialogFragment() {
    private val viewModel : ArticlesViewModel by activityViewModels()
    private val selectedCategories = mutableListOf<String>()
    private val args : ChoseCategoryDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val categories = args.categories.toList().map { "${it.title} (${it.articlesCount})" }.toTypedArray()
        val checked = BooleanArray(args.categories.size){
            args.selectedCategories.contains(args.categories[it].categoryId)
        }
        val adb =AlertDialog.Builder(requireContext())
            .setTitle("ChooseCategory")
            .setPositiveButton("Apply"){ _, _ ->
                viewModel.applyCategories(selectedCategories)
            }
            .setNegativeButton("Reset"){ _, _ ->
                viewModel.applyCategories(emptyList())
            }
            .setMultiChoiceItems(categories, checked){dialog, which, isChecked ->
                if (isChecked) selectedCategories.add(args.categories[which].categoryId)
                else selectedCategories.remove(args.categories[which].categoryId)
            }

        return adb.create()
    }
}