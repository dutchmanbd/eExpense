package backbencers.nub.dailycostcalc.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import backbencers.nub.dailycostcalc.R;
import backbencers.nub.dailycostcalc.constant.Constant;
import backbencers.nub.dailycostcalc.database.ExpenseDataSource;
import backbencers.nub.dailycostcalc.model.Category;
import backbencers.nub.dailycostcalc.model.Credit;

public class CreditEditorActivity extends AppCompatActivity {

    private String TAG = CreditEditorActivity.class.getSimpleName();

    private static EditText etCreditDate;
    private ImageButton ibCalendar;
    private AutoCompleteTextView actvCreditCategory;
    private EditText etCreditDescription;
    private EditText etCreditAmount;
    private ExpenseDataSource expenseDataSource;
    private ArrayList<String> cat = new ArrayList<>();
    private Intent intent;
    private String activityType;
    private int creditId;
    private boolean creditHasChanged = false;
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            creditHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_editor);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        expenseDataSource = new ExpenseDataSource(this);
        initializeViews();
        ibCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });
        ArrayList<String> categories = getCategoriesFromDatabase();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, categories);
        actvCreditCategory.setAdapter(adapter);
        actvCreditCategory.setThreshold(1);
        intent = getIntent();
        activityType = intent.getStringExtra(Constant.ACTIVITY_TYPE);
        Log.e(TAG, "Activity type: " + activityType);

        if (activityType.equals(Constant.ACTIVITY_TYPE_ADD)) {
            setTitle("Add Credit");
            invalidateOptionsMenu();
            setInitialDate();
        } else if (activityType.equals(Constant.ACTIVITY_TYPE_EDIT)) {
            setTitle("Edit Credit");
            creditId = intent.getIntExtra(Constant.CREDIT_ITEM_ID, -1);
            Log.e(TAG, "credit list item position: " + creditId);
            if (creditId > -1) {
                Credit credit = expenseDataSource.getCredit(creditId);
                etCreditDate.setText(credit.getCreditDate());
                actvCreditCategory.setText(credit.getCreditCategory());
                etCreditDescription.setText(credit.getCreditDescription());
                etCreditAmount.setText("" + credit.getCreditAmount());
            } else {
                Toast.makeText(this, "Error loading credit!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ArrayList<String> getCategoriesFromDatabase() {
        ArrayList<Category> categories = expenseDataSource.getAllCategories();

        for (int i = 0; i < categories.size(); i++) {
            String c = categories.get(i).getCategoryName();
            cat.add(c);
        }

        return cat;
    }

    private void initializeViews() {
        etCreditDate = (EditText) findViewById(R.id.edit_text_date);
        ibCalendar = (ImageButton) findViewById(R.id.image_button_calendar);
        actvCreditCategory = (AutoCompleteTextView) findViewById(R.id.auto_complete_category);
        etCreditDescription = (EditText) findViewById(R.id.edit_text_description);
        etCreditAmount = (EditText) findViewById(R.id.edit_text_amount);

        etCreditDate.setOnTouchListener(touchListener);
        ibCalendar.setOnTouchListener(touchListener);
        actvCreditCategory.setOnTouchListener(touchListener);
        etCreditDescription.setOnTouchListener(touchListener);
        etCreditAmount.setOnTouchListener(touchListener);
    }

    private void setInitialDate() {
        String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        etCreditDate.setText(date);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_credit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                // If the credit hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!creditHasChanged) {
                    NavUtils.navigateUpFromSameTask(CreditEditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(CreditEditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
            case R.id.action_save_credit:
                saveCredit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the credit hasn't changed, continue with handling back button press
        if (!creditHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void saveCredit() {
        String date = etCreditDate.getText().toString().trim();
        String category = actvCreditCategory.getText().toString().trim();
        String description = etCreditDescription.getText().toString().trim();
        String amount = etCreditAmount.getText().toString().trim();

        if (TextUtils.isEmpty(date)) {
            Toast.makeText(this, "Please enter or select a date!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please enter a category!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please enter description!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(amount)) {
            Toast.makeText(this, "Please enter amount!", Toast.LENGTH_SHORT).show();
        } else {

            if (!isExistedCategory(category)) {
                expenseDataSource.insertCategory(new Category(category));
            }

            Credit credit = new Credit(date, category, description, new Double(amount), (int) (System.currentTimeMillis() % 100000000));
            Log.e(TAG, "system currentTimeMillis: " + System.currentTimeMillis() % 100000000);

            if (activityType.equals(Constant.ACTIVITY_TYPE_ADD)) {
                boolean inserted = expenseDataSource.insertCredit(credit);
                if (inserted) {
                    Toast.makeText(this, "Credit saved!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save credit!", Toast.LENGTH_SHORT).show();
                }
            } else if (activityType.equals(Constant.ACTIVITY_TYPE_EDIT)) {
                boolean updated = expenseDataSource.updateCredit(creditId, credit);
                if (updated) {
                    Toast.makeText(this, "Credit updated!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update credit!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean isExistedCategory(String category) {
        for (String s : cat) {
            if (s.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Do something with the date chosen by the user
            month += 1;
            String finalDay = day > 9 ? ("" + day) : ("0" + day);
            String finalMonth = month > 9 ? ("" + month) : ("0" + month);
            String finalYear = "" + year;
            etCreditDate.setText("");
            etCreditDate.setText(finalDay + "-" + finalMonth + "-" + finalYear);
        }
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
