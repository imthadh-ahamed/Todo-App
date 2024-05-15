package com.coding.meet.todo_app

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Query
import com.coding.meet.todo_app.adapters.TaskRVVBListAdapter
import com.coding.meet.todo_app.databinding.ActivityMainBinding
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.utils.Status
import com.coding.meet.todo_app.utils.StatusResult
import com.coding.meet.todo_app.utils.StatusResult.Added
import com.coding.meet.todo_app.utils.StatusResult.Deleted
import com.coding.meet.todo_app.utils.StatusResult.Updated
import com.coding.meet.todo_app.utils.clearEditText
import com.coding.meet.todo_app.utils.hideKeyBoard
import com.coding.meet.todo_app.utils.longToastShow
import com.coding.meet.todo_app.utils.setupDialog
import com.coding.meet.todo_app.utils.validateEditText
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // View binding for the activity
    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Dialogs for adding tasks
    private val addTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.add_task_dialog)
        }
    }

    // Dialogs for updating tasks
    private val updateTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.update_task_dialog)
        }
    }

    // Dialogs for loading tasks
    private val loadingDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }

    // ViewModel for managing tasks
    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(this)[TaskViewModel::class.java]
    }

    // LiveData for managing list or grid view
    private val isListMutableLiveData = MutableLiveData<Boolean>().apply {
        postValue(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view using view binding
        setContentView(mainBinding.root)


        // Add task dialog setup
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        // Dismiss the dialog when close button is clicked
        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }

        // Find the title EditText and its layout in the add task dialog
        val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)

        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETTitle, addETTitleL)
            }

        })

        // Find the description EditText and its layout in the add task dialog
        val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETDesc, addETDescL)
            }
        })

        // Set click listener for the FAB button to show the add task dialog
        mainBinding.addTaskFABtn.setOnClickListener {
            // Clear the EditText fields and show the add task dialog
            clearEditText(addETTitle, addETTitleL)
            clearEditText(addETDesc, addETDescL)
            addTaskDialog.show()
        }

        // Find the save button in the add task dialog
        val saveTaskBtn = addTaskDialog.findViewById<Button>(R.id.saveTaskBtn)
        saveTaskBtn.setOnClickListener {
            if (validateEditText(addETTitle, addETTitleL)
                && validateEditText(addETDesc, addETDescL)
            ) {
                // Create a new Task object with input data and current date
                val newTask = Task(
                    UUID.randomUUID().toString(),
                    addETTitle.text.toString().trim(),
                    addETDesc.text.toString().trim(),
                    Date()
                )
                // Hide the keyboard, dismiss the dialog, and insert the new task using ViewModel
                hideKeyBoard(it)
                addTaskDialog.dismiss()
                taskViewModel.insertTask(newTask)
            }
        }
        // Add task end


        // Update task dialog setup
        // Find the title and description EditTexts and their layouts
        val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)

        updateETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETTitle, updateETTitleL)
            }

        })

        val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

        updateETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETDesc, updateETDescL)
            }
        })

        // Find the close button in the update task dialog and set its click listener to dismiss the dialog
        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }

        val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)

        // Update Task End

        // Observing list or grid view toggle
        isListMutableLiveData.observe(this){
            if (it){
                mainBinding.taskRV.layoutManager = LinearLayoutManager(
                    this,LinearLayoutManager.VERTICAL,false
                )
                mainBinding.listOrGridImg.setImageResource(R.drawable.ic_view_module)
            }else{
                mainBinding.taskRV.layoutManager = StaggeredGridLayoutManager(
                    2,LinearLayoutManager.VERTICAL
                )
                mainBinding.listOrGridImg.setImageResource(R.drawable.ic_view_list)
            }
        }

        // Set click listener for the list/grid view toggle image
        mainBinding.listOrGridImg.setOnClickListener {
            isListMutableLiveData.postValue(!isListMutableLiveData.value!!)
        }

        // Create a TaskRVVBListAdapter and set it as the adapter for RecyclerView
        val taskRVVBListAdapter = TaskRVVBListAdapter(isListMutableLiveData ) { type, position, task ->
            if (type == "delete") {
                taskViewModel
                    // Deleted Task
//                .deleteTask(task)
                    .deleteTaskUsingId(task.id)

                // Restore Deleted task
                restoreDeletedTask(task)
            } else if (type == "update") {
                // Update task action
                updateETTitle.setText(task.title)
                updateETDesc.setText(task.description)
                updateTaskBtn.setOnClickListener {
                    if (validateEditText(updateETTitle, updateETTitleL)
                        && validateEditText(updateETDesc, updateETDescL)
                    ) {
                        // Create an updated Task object and update it using ViewModel
                        val updateTask = Task(
                            task.id,
                            updateETTitle.text.toString().trim(),
                            updateETDesc.text.toString().trim(),
//                           here i Date updated
                            Date()
                        )
                        // Hide the keyboard, dismiss the dialog, and update the task using ViewModel
                        hideKeyBoard(it)
                        updateTaskDialog.dismiss()
                        taskViewModel
                            .updateTask(updateTask)
//                            .updateTaskPaticularField(
//                                task.id,
//                                updateETTitle.text.toString().trim(),
//                                updateETDesc.text.toString().trim()
//                            )
                    }
                }
                // Show the update task dialog
                updateTaskDialog.show()
            }
        }

        // Set the adapter for RecyclerView
        mainBinding.taskRV.adapter = taskRVVBListAdapter
        // Disable nested scrolling for RecyclerView
        ViewCompat.setNestedScrollingEnabled(mainBinding.taskRV,false)
        taskRVVBListAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
//                mainBinding.taskRV.smoothScrollToPosition(positionStart)
                mainBinding.nestedScrollView.smoothScrollTo(0,positionStart)
            }
        })
        callGetTaskList(taskRVVBListAdapter)
        callSortByLiveData()
        statusCallback()

        callSearch()

    }

    // Function to restore a deleted task
    private fun restoreDeletedTask(deletedTask : Task){
        // Create a Snackbar with a message indicating the deleted task's title
        val snackBar = Snackbar.make(
            mainBinding.root, "Deleted '${deletedTask.title}'",
            Snackbar.LENGTH_LONG
        )
        // Set an action on the Snackbar to undo the delete operation
        snackBar.setAction("Undo"){
            // Insert the deleted task back using the ViewModel
            taskViewModel.insertTask(deletedTask)
        }
        // Show the Snackbar
        snackBar.show()
    }

    // Function to handle search functionality
    private fun callSearch() {
        // Add a text changed listener to the search EditText
        mainBinding.edSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(query: Editable) {
                if (query.toString().isNotEmpty()){
                    // Perform search if the query is not empty
                    taskViewModel.searchTaskList(query.toString())
                }else{
                    // Call the sorting method if the query is empty
                    callSortByLiveData()
                }
            }
        })

        // Set an editor action listener for the search EditText
        mainBinding.edSearch.setOnEditorActionListener{ v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                // Hide the keyboard when the search action is triggered
                hideKeyBoard(v)
                return@setOnEditorActionListener true
            }
            false
        }

        // Call the method to show the sorting dialog
        callSortByDialog()
    }

    // Function to observe LiveData for sorting tasks
    private fun callSortByLiveData(){
        taskViewModel.sortByLiveData.observe(this){
            // Get the sorted task list based on the LiveData values
            taskViewModel.getTaskList(it.second,it.first)
        }
    }

    // Function to display a dialog for sorting tasks
    private fun callSortByDialog() {
        var checkedItem = 0   // 2 is default item set
        val items = arrayOf("Title Ascending", "Title Descending","Date Ascending","Date Descending")

        // Set a click listener for the sort button
        mainBinding.sortImg.setOnClickListener {
            // Create a MaterialAlertDialogBuilder
            MaterialAlertDialogBuilder(this)
                .setTitle("Sort By")
                .setPositiveButton("Ok") { _, _ ->
                    // When the "Ok" button is clicked, set sorting parameters based on the selected item
                    when (checkedItem) {
                        0 -> {
                            taskViewModel.setSortBy(Pair("title",true))
                        }
                        1 -> {
                            taskViewModel.setSortBy(Pair("title",false))
                        }
                        2 -> {
                            taskViewModel.setSortBy(Pair("date",true))
                        }
                        else -> {
                            taskViewModel.setSortBy(Pair("date",false))
                        }
                    }
                }
                // Set the single choice items for sorting options
                .setSingleChoiceItems(items, checkedItem) { _, selectedItemIndex ->
                    checkedItem = selectedItemIndex
                }
                .setCancelable(false)
                .show()
        }
    }

    // Function to handle status callbacks
    private fun statusCallback() {
        // Observe the status LiveData from the ViewModel
        taskViewModel
            .statusLiveData
            .observe(this) {
                when (it.status) {
                    Status.LOADING -> {
                        // Show loading dialog when status is loading
                        loadingDialog.show()
                    }

                    Status.SUCCESS -> {
                        // Dismiss loading dialog on success and handle status result
                        loadingDialog.dismiss()
                        when (it.data as StatusResult) {
                            Added -> {
                                Log.d("StatusResult", "Added")
                            }

                            Deleted -> {
                                Log.d("StatusResult", "Deleted")

                            }

                            Updated -> {
                                Log.d("StatusResult", "Updated")

                            }
                        }
                        // Show a message using toast
                        it.message?.let { it1 -> longToastShow(it1) }
                    }

                    Status.ERROR -> {
                        // Dismiss loading dialog on error and show error message using toast
                        loadingDialog.dismiss()
                        it.message?.let { it1 -> longToastShow(it1) }
                    }
                }
            }
    }

    // Function to fetch and display task list
    private fun callGetTaskList(taskRecyclerViewAdapter: TaskRVVBListAdapter) {
        // Launch a coroutine in the main dispatcher
        CoroutineScope(Dispatchers.Main).launch {
            // Observe the task state flow from the ViewModel
            taskViewModel
                .taskStateFlow
                .collectLatest {
                    Log.d("status", it.status.toString())

                    when (it.status) {
                        Status.LOADING -> {
                            // Show loading dialog when status is loading
                            loadingDialog.show()
                        }

                        Status.SUCCESS -> {
                            // Dismiss loading dialog on success and submit the task list to the adapter
                            loadingDialog.dismiss()
                            it.data?.collect { taskList ->
                                taskRecyclerViewAdapter.submitList(taskList)
                            }
                        }

                        Status.ERROR -> {
                            // Dismiss loading dialog on error and show error message using toast
                            loadingDialog.dismiss()
                            it.message?.let { it1 -> longToastShow(it1) }
                        }
                    }

                }
        }
    }
}