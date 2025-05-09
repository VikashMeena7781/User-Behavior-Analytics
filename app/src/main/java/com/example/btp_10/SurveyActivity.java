package com.example.btp_10;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SurveyActivity extends AppCompatActivity {

    // Firebase
    private DatabaseReference databaseRef;
    private FirebaseAuth mAuth;

    // UI Components - Mood & Emotions
    private RadioGroup rgInterest, rgDepressed, rgSleepTroubles, rgTired, rgAppetite,
            rgSelfEsteem, rgConcentration, rgMovement, rgSuicidal;

    // UI Components - Sleep Patterns
    private TimePicker timePickerBedTime;
    private SeekBar sliderSleepHours;
    private TextView tvSleepHours;
    private RadioGroup rgFallingAsleep, rgSleepQuality;

    // UI Components - Social Connection
    private RadioGroup rgIsolated, rgInTune, rgCompanionship, rgMeaningfulConversations;

    // UI Components - Stress & Coping
    private RadioGroup rgNervous, rgConfident, rgOverwhelmed;
    private CheckBox cbTalking, cbExercise, cbAvoiding, cbPositive, cbHumor, cbNone;

    // UI Components - Productivity & Focus
    private SeekBar sliderFocus, sliderProductivity;
    private TextView tvFocusValue, tvProductivityValue;
    private RadioGroup rgCompleteTasks;

    // UI Components - Physical Health & Energy
    private RadioGroup rgExerciseWeek;
    private NumberPicker numberPickerExerciseDays;
    private SeekBar sliderEnergy;
    private TextView tvEnergyValue;
    private CheckBox cbHeadaches, cbStomach, cbMuscle, cbFatigue, cbNoneSymptoms;

    // Submit Button
    private Button buttonSubmitSurvey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("surveys");

        // Initialize UI components
        initializeComponents();
        setupSliders();
        setupNumberPicker();
        setupCopingCheckboxes();
        setupSymptomCheckboxes();

        // Setup button
        buttonSubmitSurvey.setOnClickListener(v -> submitSurvey());
    }

    private void initializeComponents() {
        // Mood & Emotions
        rgInterest = findViewById(R.id.rgInterest);
        rgDepressed = findViewById(R.id.rgDepressed);
        rgSleepTroubles = findViewById(R.id.rgSleepTroubles);
        rgTired = findViewById(R.id.rgTired);
        rgAppetite = findViewById(R.id.rgAppetite);
        rgSelfEsteem = findViewById(R.id.rgSelfEsteem);
        rgConcentration = findViewById(R.id.rgConcentration);
        rgMovement = findViewById(R.id.rgMovement);
        rgSuicidal = findViewById(R.id.rgSuicidal);

        // Sleep Patterns
        timePickerBedTime = findViewById(R.id.timePickerBedTime);
        sliderSleepHours = findViewById(R.id.sliderSleepHours);
        tvSleepHours = findViewById(R.id.tvSleepHours);
        rgFallingAsleep = findViewById(R.id.rgFallingAsleep);
        rgSleepQuality = findViewById(R.id.rgSleepQuality);

        // Social Connection
        rgIsolated = findViewById(R.id.rgIsolated);
        rgInTune = findViewById(R.id.rgInTune);
        rgCompanionship = findViewById(R.id.rgCompanionship);
        rgMeaningfulConversations = findViewById(R.id.rgMeaningfulConversations);

        // Stress & Coping
        rgNervous = findViewById(R.id.rgNervous);
        rgConfident = findViewById(R.id.rgConfident);
        rgOverwhelmed = findViewById(R.id.rgOverwhelmed);
        cbTalking = findViewById(R.id.cbTalking);
        cbExercise = findViewById(R.id.cbExercise);
        cbAvoiding = findViewById(R.id.cbAvoiding);
        cbPositive = findViewById(R.id.cbPositive);
        cbHumor = findViewById(R.id.cbHumor);
        cbNone = findViewById(R.id.cbNone);

        // Productivity & Focus
        sliderFocus = findViewById(R.id.sliderFocus);
        sliderProductivity = findViewById(R.id.sliderProductivity);
        tvFocusValue = findViewById(R.id.tvFocusValue);
        tvProductivityValue = findViewById(R.id.tvProductivityValue);
        rgCompleteTasks = findViewById(R.id.rgCompleteTasks);

        // Physical Health & Energy
        rgExerciseWeek = findViewById(R.id.rgExerciseWeek);
        numberPickerExerciseDays = findViewById(R.id.numberPickerExerciseDays);
        sliderEnergy = findViewById(R.id.sliderEnergy);
        tvEnergyValue = findViewById(R.id.tvEnergyValue);
        cbHeadaches = findViewById(R.id.cbHeadaches);
        cbStomach = findViewById(R.id.cbStomach);
        cbMuscle = findViewById(R.id.cbMuscle);
        cbFatigue = findViewById(R.id.cbFatigue);
        cbNoneSymptoms = findViewById(R.id.cbNoneSymptoms);

        // Submit Button
        buttonSubmitSurvey = findViewById(R.id.buttonSubmitSurvey);
    }

    private void setupSliders() {
        // Sleep Hours Slider (values 4-12 hours)
        sliderSleepHours.setMax(16); // 16 half-steps for 4-12 hours
        sliderSleepHours.setProgress(8); // Default to 8 hours
        sliderSleepHours.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = 4 + (progress * 0.5f); // Convert to 4-12 range with 0.5 steps
                tvSleepHours.setText(value + " hours");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Focus Slider (values 1-5)
        sliderFocus.setMax(4);
        sliderFocus.setProgress(2); // Default 3 (middle value)
        sliderFocus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 1; // Convert to 1-5 range
                tvFocusValue.setText(String.valueOf(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Productivity Slider (values 1-5)
        sliderProductivity.setMax(4);
        sliderProductivity.setProgress(2); // Default 3
        sliderProductivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 1; // Convert to 1-5 range
                tvProductivityValue.setText(String.valueOf(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Energy Slider (values 1-5)
        sliderEnergy.setMax(4);
        sliderEnergy.setProgress(2); // Default 3
        sliderEnergy.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 1; // Convert to 1-5 range
                tvEnergyValue.setText(String.valueOf(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupNumberPicker() {
        // Configure Exercise Days NumberPicker
        numberPickerExerciseDays.setMinValue(0);
        numberPickerExerciseDays.setMaxValue(7);

        // Set default visibility based on radio selection
        rgExerciseWeek.setOnCheckedChangeListener((group, checkedId) -> {
            numberPickerExerciseDays.setVisibility(
                    checkedId == R.id.rbExerciseWeekYes ? View.VISIBLE : View.GONE);
        });

        // Initialize number picker visibility
        numberPickerExerciseDays.setVisibility(View.GONE);
    }

    private void setupCopingCheckboxes() {
        // Set mutual exclusivity between "None" and other checkboxes in coping strategies
        cbNone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbTalking.setChecked(false);
                cbExercise.setChecked(false);
                cbAvoiding.setChecked(false);
                cbPositive.setChecked(false);
                cbHumor.setChecked(false);
            }
        });

        // Make other options uncheck "None" when selected
        CheckBox[] copingOptions = {cbTalking, cbExercise, cbAvoiding, cbPositive, cbHumor};
        for (CheckBox option : copingOptions) {
            option.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cbNone.setChecked(false);
                }
            });
        }
    }

    private void setupSymptomCheckboxes() {
        // Similar for physical symptoms
        cbNoneSymptoms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbHeadaches.setChecked(false);
                cbStomach.setChecked(false);
                cbMuscle.setChecked(false);
                cbFatigue.setChecked(false);
            }
        });

        // Make other options uncheck "None" when selected
        CheckBox[] symptomOptions = {cbHeadaches, cbStomach, cbMuscle, cbFatigue};
        for (CheckBox option : symptomOptions) {
            option.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cbNoneSymptoms.setChecked(false);
                }
            });
        }
    }

    private boolean validateFields() {
        // Mood & Emotions validation
        if (rgInterest.getCheckedRadioButtonId() == -1 ||
                rgDepressed.getCheckedRadioButtonId() == -1 ||
                rgSleepTroubles.getCheckedRadioButtonId() == -1 ||
                rgTired.getCheckedRadioButtonId() == -1 ||
                rgAppetite.getCheckedRadioButtonId() == -1 ||
                rgSelfEsteem.getCheckedRadioButtonId() == -1 ||
                rgConcentration.getCheckedRadioButtonId() == -1 ||
                rgMovement.getCheckedRadioButtonId() == -1 ||
                rgSuicidal.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please complete all mood & emotions questions", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Sleep Patterns validation
        if (rgFallingAsleep.getCheckedRadioButtonId() == -1 ||
                rgSleepQuality.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please complete all sleep pattern questions", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Social Connection validation
        if (rgIsolated.getCheckedRadioButtonId() == -1 ||
                rgInTune.getCheckedRadioButtonId() == -1 ||
                rgCompanionship.getCheckedRadioButtonId() == -1 ||
                rgMeaningfulConversations.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please complete all social connection questions", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Stress & Coping validation
        if (rgNervous.getCheckedRadioButtonId() == -1 ||
                rgConfident.getCheckedRadioButtonId() == -1 ||
                rgOverwhelmed.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please complete all stress & coping questions", Toast.LENGTH_SHORT).show();
            return false;
        }

        // At least one coping strategy (or none) must be selected
        if (!cbTalking.isChecked() && !cbExercise.isChecked() && !cbAvoiding.isChecked() &&
                !cbPositive.isChecked() && !cbHumor.isChecked() && !cbNone.isChecked()) {
            Toast.makeText(this, "Please select at least one coping strategy or 'None'", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Productivity validation
        if (rgCompleteTasks.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please answer if you completed your tasks", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Physical Health validation
        if (rgExerciseWeek.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please answer if you exercised this week", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Verify exercise days is shown if Yes is selected
        if (rgExerciseWeek.getCheckedRadioButtonId() == R.id.rbExerciseWeekYes &&
                numberPickerExerciseDays.getVisibility() != View.VISIBLE) {
            numberPickerExerciseDays.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Please specify how many days you exercised", Toast.LENGTH_SHORT).show();
            return false;
        }

        // At least one symptom (or none) must be selected
        if (!cbHeadaches.isChecked() && !cbStomach.isChecked() && !cbMuscle.isChecked() &&
                !cbFatigue.isChecked() && !cbNoneSymptoms.isChecked()) {
            Toast.makeText(this, "Please select at least one physical symptom or 'None'", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void submitSurvey() {
        // Validate fields first
        if (!validateFields()) {
            return;
        }

        // Disable submit button to prevent multiple submissions
        buttonSubmitSurvey.setEnabled(false);

        // Create a map with all survey responses
        Map<String, Object> surveyData = new HashMap<>();

        // Metadata Section
        Map<String, Object> metaData = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date());
        metaData.put("timestamp", formattedDate);
        metaData.put("timestampRaw", System.currentTimeMillis());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            metaData.put("userId", currentUser.getUid());
            metaData.put("userEmail", currentUser.getEmail());
        }
        surveyData.put("metadata", metaData);

        // Mood & Emotions Section
        Map<String, Object> moodEmotionsData = new HashMap<>();
        moodEmotionsData.put("interest", getSelectedRadioValue(rgInterest));
        moodEmotionsData.put("depressed", getSelectedRadioValue(rgDepressed));
        moodEmotionsData.put("sleepTroubles", getSelectedRadioValue(rgSleepTroubles));
        moodEmotionsData.put("tired", getSelectedRadioValue(rgTired));
        moodEmotionsData.put("appetite", getSelectedRadioValue(rgAppetite));
        moodEmotionsData.put("selfEsteem", getSelectedRadioValue(rgSelfEsteem));
        moodEmotionsData.put("concentration", getSelectedRadioValue(rgConcentration));
        moodEmotionsData.put("movement", getSelectedRadioValue(rgMovement));
        moodEmotionsData.put("suicidal", getSelectedRadioValue(rgSuicidal));

        // Calculate PHQ-9 score (depression assessment)
        int phq9Score = getSelectedRadioValue(rgInterest) +
                getSelectedRadioValue(rgDepressed) +
                getSelectedRadioValue(rgSleepTroubles) +
                getSelectedRadioValue(rgTired) +
                getSelectedRadioValue(rgAppetite) +
                getSelectedRadioValue(rgSelfEsteem) +
                getSelectedRadioValue(rgConcentration) +
                getSelectedRadioValue(rgMovement) +
                getSelectedRadioValue(rgSuicidal);
        moodEmotionsData.put("phq9Score", phq9Score);

        // Depression severity based on PHQ-9 score
        String depressionSeverity;
        if (phq9Score >= 0 && phq9Score <= 4) depressionSeverity = "None/Minimal";
        else if (phq9Score >= 5 && phq9Score <= 9) depressionSeverity = "Mild";
        else if (phq9Score >= 10 && phq9Score <= 14) depressionSeverity = "Moderate";
        else if (phq9Score >= 15 && phq9Score <= 19) depressionSeverity = "Moderately Severe";
        else depressionSeverity = "Severe";
        moodEmotionsData.put("depressionSeverity", depressionSeverity);

        surveyData.put("moodEmotions", moodEmotionsData);

        // Sleep Patterns Section
        Map<String, Object> sleepData = new HashMap<>();
        sleepData.put("bedTimeHour", timePickerBedTime.getHour());
        sleepData.put("bedTimeMinute", timePickerBedTime.getMinute());
        float sleepHours = 4 + (sliderSleepHours.getProgress() * 0.5f);
        sleepData.put("sleepHours", sleepHours);
        sleepData.put("fallingAsleep", getSelectedRadioValue(rgFallingAsleep));
        sleepData.put("sleepQuality", getSelectedRadioValue(rgSleepQuality));
        surveyData.put("sleepPatterns", sleepData);

        // Social Connection Section
        Map<String, Object> socialData = new HashMap<>();
        socialData.put("isolated", getSelectedRadioValue(rgIsolated));
        socialData.put("inTune", getSelectedRadioValue(rgInTune));
        socialData.put("companionship", getSelectedRadioValue(rgCompanionship));
        socialData.put("meaningfulConversations", getSelectedRadioValue(rgMeaningfulConversations));
        surveyData.put("socialConnection", socialData);

        // Stress & Coping Section
        Map<String, Object> stressData = new HashMap<>();
        stressData.put("nervous", getSelectedRadioValue(rgNervous));
        stressData.put("confident", getSelectedRadioValue(rgConfident));
        stressData.put("overwhelmed", getSelectedRadioValue(rgOverwhelmed));

        // Coping strategies
        List<String> copingStrategies = new ArrayList<>();
        if (cbTalking.isChecked()) copingStrategies.add("Talking to someone");
        if (cbExercise.isChecked()) copingStrategies.add("Physical exercise");
        if (cbAvoiding.isChecked()) copingStrategies.add("Avoiding the problem");
        if (cbPositive.isChecked()) copingStrategies.add("Finding something positive");
        if (cbHumor.isChecked()) copingStrategies.add("Using humor");
        if (cbNone.isChecked()) copingStrategies.add("None");
        stressData.put("copingStrategies", copingStrategies);
        surveyData.put("stressCoping", stressData);

        // Productivity & Focus Section
        Map<String, Object> productivityData = new HashMap<>();
        productivityData.put("focusRating", sliderFocus.getProgress() + 1);
        productivityData.put("productivityRating", sliderProductivity.getProgress() + 1);
        boolean completedTasks = rgCompleteTasks.getCheckedRadioButtonId() == R.id.rbCompleteTasksYes;
        productivityData.put("completedTasks", completedTasks);
        surveyData.put("productivityFocus", productivityData);

        // Physical Health & Energy Section
        Map<String, Object> physicalHealthData = new HashMap<>();
        boolean didExercise = rgExerciseWeek.getCheckedRadioButtonId() == R.id.rbExerciseWeekYes;
        physicalHealthData.put("didExercise", didExercise);
        physicalHealthData.put("exerciseDays", didExercise ? numberPickerExerciseDays.getValue() : 0);
        physicalHealthData.put("energyLevel", sliderEnergy.getProgress() + 1);

        // Physical symptoms
        List<String> physicalSymptoms = new ArrayList<>();
        if (cbHeadaches.isChecked()) physicalSymptoms.add("Headaches");
        if (cbStomach.isChecked()) physicalSymptoms.add("Stomach issues");
        if (cbMuscle.isChecked()) physicalSymptoms.add("Muscle tension");
        if (cbFatigue.isChecked()) physicalSymptoms.add("Fatigue");
        if (cbNoneSymptoms.isChecked()) physicalSymptoms.add("None");
        physicalHealthData.put("physicalSymptoms", physicalSymptoms);
        surveyData.put("physicalHealth", physicalHealthData);

        // Get current user
        currentUser = mAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Create a unique key for this survey under the user's ID
            String surveyKey = databaseRef.child(userId).push().getKey();

            // Save to Firebase Realtime Database
            if (surveyKey != null) {
                databaseRef.child(userId).child(surveyKey)
                        .setValue(surveyData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(SurveyActivity.this, "Survey submitted successfully!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SurveyActivity.this, "Error submitting survey: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                buttonSubmitSurvey.setEnabled(true);
                            }
                        });
            } else {
                Toast.makeText(SurveyActivity.this, "Error generating survey ID",
                        Toast.LENGTH_SHORT).show();
                buttonSubmitSurvey.setEnabled(true);
            }
        } else {
            // Handle case where user is not logged in
            Toast.makeText(SurveyActivity.this, "Please log in to submit surveys",
                    Toast.LENGTH_SHORT).show();
            // Redirect to login screen
            startActivity(new Intent(SurveyActivity.this, LoginActivity.class));
            buttonSubmitSurvey.setEnabled(true);
        }
    }

    /**
     * Helper method to get the value from a radio group
     */
    private int getSelectedRadioValue(RadioGroup radioGroup) {
        int radioButtonID = radioGroup.getCheckedRadioButtonId();
        if (radioButtonID == -1) return -1; // No selection

        RadioButton radioButton = findViewById(radioButtonID);
        try {
            return Integer.parseInt(radioButton.getText().toString());
        } catch (NumberFormatException e) {
            // For Yes/No radiobuttons, return 1 for Yes, 0 for No
            return radioButton.getText().toString().equalsIgnoreCase("Yes") ? 1 : 0;
        }
    }
}