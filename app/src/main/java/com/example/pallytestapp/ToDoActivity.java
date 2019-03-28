package com.example.pallytestapp;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread;

public class ToDoActivity extends Fragment{

    /**
     * Client reference
     */
    private MobileServiceClient mClient;
    /**
     * Table used to access data from the mobile app backend.
     */
    private MobileServiceTable<ToDoItem> mToDoTable;
    /**
     * App URL for conenction
     */
    private String appStringUri = "https://pallytestapp.azurewebsites.net";
    /**
     * Offline Sync:
     * Table used to store data locally sync with the mobile app backend.
     */
    //private MobileServiceSyncTable<ToDoItem> mToDoTable;
    /**
     * Adapter to sync the items list with the view
     */
    private ToDoItemAdapter mAdapter;
    /**
     * EditText containing the "New To Do" text
     */
    private EditText mTextNewToDo;
    /**
     * Progress spinner to use for table operations
     */
    private ProgressBar mProgressBar;

    protected BottomNavigationView btNavView;

    public static ToDoActivity newInstance() {
        ToDoActivity fragment = new ToDoActivity();
        return fragment;
    }

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.todolist_main_scene, container, false);

        mProgressBar = v.findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            mClient = new MobileServiceClient(appStringUri,
                    getActivity().getApplicationContext()).withFilter(new ProgressFilter());

            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });

            mToDoTable = mClient.getTable(ToDoItem.class);

            initLocalStore().get();
            mTextNewToDo = getView().findViewById(R.id.textNewToDo);

            mAdapter = new ToDoItemAdapter(getActivity(), R.layout.row_list_to_do, this);

            RecyclerView listViewToDo = getView().findViewById(R.id.listViewToDo);
            listViewToDo.setAdapter(mAdapter);
            listViewToDo.setLayoutManager(new LinearLayoutManager(getActivity()));

            refreshItemsFromTable();

            FloatingActionButton fab = getView().findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refreshItemsFromTable();
                }
            });

        }
        catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        }
        catch (Exception e){
            createAndShowDialog(e, "Error");
        }
    }
    /**
     * Mark an item as completed
     *
     * @param item
     *            The item to mark
     */
    public void checkItem(final ToDoItem item) {
        if (mClient == null) {
            return;
        }
        item.setComplete(true);
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
            try {
                checkItemInTable(item);
            }
            catch (final Exception e) {
                createAndShowDialogFromTask(e, "Error");
            }
            return null;
            }
        };
        runAsyncTask(task);
    }

    /**
     * Mark an item as completed in the Mobile Service Table
     *
     * @param item
     *            The item to mark
     */
    public void checkItemInTable(ToDoItem item) throws ExecutionException, InterruptedException {
        mToDoTable.update(item).get();
    }

    public void addItem(View view) {
        if (mClient == null) {
            return;
        }

        final ToDoItem item = new ToDoItem();
        item.setText(mTextNewToDo.getText().toString());
        item.setComplete(false);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
            try {
                final ToDoItem entity = addItemInTable(item);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    if(!entity.isComplete()){
                        mAdapter.addList(entity);
                        mAdapter.notifyItemInserted(0);
                    }
                    }
                });
            }
            catch (final Exception e) {
                createAndShowDialogFromTask(e, "Error");
            }
            return null;
            }
        };
        runAsyncTask(task);
        mTextNewToDo.setText("");
    }

    /**
     * Add an item to the Mobile Service Table
     *
     * @param item
     *            The item to Add
     */
    public ToDoItem addItemInTable(ToDoItem item) throws ExecutionException, InterruptedException {
        ToDoItem entity = mToDoTable.insert(item).get();
        return entity;
    }

    /**
     * Refresh the list with the items in the Table
     */
    private void refreshItemsFromTable() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
            try {
                final ArrayList<ToDoItem> results = refreshItemsFromMobileServiceTable();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.clearList();
                        mAdapter.notifyDataSetChanged();
                        int i = 0;
                        for (ToDoItem item : results) {
                            mAdapter.addList(item);
                            mAdapter.notifyItemInserted(i);
                            i++;
                        }
                    }
                });
            }
            catch (final Exception e){
                createAndShowDialogFromTask(e, "Error");
            }
            return null;
            }
        };
        runAsyncTask(task);
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */
    private ArrayList<ToDoItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException {
        return mToDoTable.orderBy("text", QueryOrder.Ascending).execute().get();
    }

    /**
     * Initialize local storage
     * @return
     * @throws MobileServiceLocalStoreException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private AsyncTask<Void, Void, Void> initLocalStore() throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
            try {
                MobileServiceSyncContext syncContext = mClient.getSyncContext();
                if (syncContext.isInitialized()) return null;
                SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                tableDefinition.put("id", ColumnDataType.String);
                tableDefinition.put("text", ColumnDataType.String);
                tableDefinition.put("complete", ColumnDataType.Boolean);

                localStore.defineTable("ToDoItem", tableDefinition);
                SimpleSyncHandler handler = new SimpleSyncHandler();
                syncContext.initialize(localStore, handler).get();
            }
            catch (final Exception e) {
                createAndShowDialogFromTask(e, "Error");
            }
            return null;
            }
        };
        return runAsyncTask(task);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * Run an ASync task on the corresponding executor
     * @param task
     * @return
     */
    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }

    private class ProgressFilter implements ServiceFilter {
        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();


            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

            Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>() {
                @Override
                public void onFailure(Throwable e) {
                    resultFuture.setException(e);
                }

                @Override
                public void onSuccess(ServiceFilterResponse response) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
    }
}