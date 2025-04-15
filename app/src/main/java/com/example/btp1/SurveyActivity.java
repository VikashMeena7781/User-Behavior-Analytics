package com.example.btp1;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class SurveyActivity extends AppCompatActivity {

    // UI Components
    private RatingBar ratingMood, ratingSleepQuality, ratingSocialSatisfaction,
            ratingStressLevel, ratingFocus, ratingEnergyLevels;
    private Spinner spinnerEnergeticDays, spinnerExerciseDays;
    private RadioGroup radioGroupMoodChanges, radioGroupSleepTrouble, radioGroupConnected,
            radioGroupTaskCompletion, radioGroupProductivity, radioGroupExercise,
            radioGroupPhysicalSymptoms;
    private EditText editTextMoodTriggers, editTextSleepHours, editTextConversations,
            editTextStressSource, editTextCopingStrategies, editTextSymptoms;
    private Button buttonSubmitSurvey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // Initialize UI components
        initializeComponents();
        setupSpinners();
        setupSubmitButton();
    }

    private void initializeComponents() {
        // Rating bars
        ratingMood = findViewById(R.id.ratingMood);
        ratingSleepQuality = findViewById(R.id.ratingSleepQuality);
        ratingSocialSatisfaction = findViewById(R.id.ratingSocialSatisfaction);
        ratingStressLevel = findViewById(R.id.ratingStressLevel);
        ratingFocus = findViewById(R.id.ratingFocus);
        ratingEnergyLevels = findViewById(R.id.ratingEnergyLevels);

        // Spinners
        spinnerEnergeticDays = findViewById(R.id.spinnerEnergeticDays);
        spinnerExerciseDays = findViewById(R.id.spinnerExerciseDays);

        // Radio groups
        radioGroupMoodChanges = findViewById(R.id.radioGroupMoodChanges);
        radioGroupSleepTrouble = findViewById(R.id.radioGroupSleepTrouble);
        radioGroupConnected = findViewById(R.id.radioGroupConnected);
        radioGroupTaskCompletion = findViewById(R.id.radioGroupTaskCompletion);
        radioGroupProductivity = findViewById(R.id.radioGroupProductivity);
        radioGroupExercise = findViewById(R.id.radioGroupExercise);
        radioGroupPhysicalSymptoms = findViewById(R.id.radioGroupPhysicalSymptoms);

        // EditTexts
        editTextMoodTriggers = findViewById(R.id.editTextMoodTriggers);
        editTextSleepHours = findViewById(R.id.editTextSleepHours);
        editTextConversations = findViewById(R.id.editTextConversations);
        editTextStressSource = findViewById(R.id.editTextStressSource);
        editTextCopingStrategies = findViewById(R.id.editTextCopingStrategies);
        editTextSymptoms = findViewById(R.id.editTextSymptoms);

        // Button
        buttonSubmitSurvey = findViewById(R.id.buttonSubmitSurvey);
    }

    private void setupSpinners() {
        // Create adapters for days spinners (0-7 days)
        ArrayAdapter<String> daysAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"0", "1", "2", "3", "4", "5", "6", "7"});
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerEnergeticDays.setAdapter(daysAdapter);
        spinnerExerciseDays.setAdapter(daysAdapter);
    }

    private void setupSubmitButton() {
        buttonSubmitSurvey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitSurvey();
            }
        });
    }

    private void submitSurvey() {
        // Disable the button to prevent multiple submissions
        buttonSubmitSurvey.setEnabled(false);

        // Create a map with all survey responses
        Map<String, Object> surveyData = new HashMap<>();

        // Mood Monitoring
        surveyData.put("moodRating", (int) ratingMood.getRating());
        surveyData.put("energeticDays", spinnerEnergeticDays.getSelectedItem().toString());

        int moodChangeId = radioGroupMoodChanges.getCheckedRadioButtonId();
        boolean hadMoodChanges = moodChangeId == R.id.radioMoodChangesYes;
        surveyData.put("hadMoodChanges", hadMoodChanges);
        surveyData.put("moodTriggers", editTextMoodTriggers.getText().toString());

        // Sleep Patterns
        surveyData.put("sleepQuality", (int) ratingSleepQuality.getRating());
        surveyData.put("sleepHours", editTextSleepHours.getText().toString());

        int sleepTroubleId = radioGroupSleepTrouble.getCheckedRadioButtonId();
        boolean hadSleepTrouble = sleepTroubleId == R.id.radioSleepTroubleYes;
        surveyData.put("hadSleepTrouble", hadSleepTrouble);

        // Social Interaction
        surveyData.put("socialSatisfaction", (int) ratingSocialSatisfaction.getRating());

        int connectedId = radioGroupConnected.getCheckedRadioButtonId();
        boolean feltConnected = connectedId == R.id.radioConnectedYes;
        surveyData.put("feltConnected", feltConnected);

        String conversationsText = editTextConversations.getText().toString();
        surveyData.put("meaningfulConversations", conversationsText.isEmpty() ? 0 :
                Integer.parseInt(conversationsText));

        // Stress and Coping
        surveyData.put("stressLevel", (int) ratingStressLevel.getRating());
        surveyData.put("stressSource", editTextStressSource.getText().toString());
        surveyData.put("copingStrategies", editTextCopingStrategies.getText().toString());

        // Productivity and Focus
        surveyData.put("focusRating", (int) ratingFocus.getRating());

        int tasksCompletedId = radioGroupTaskCompletion.getCheckedRadioButtonId();
        boolean completedTasks = tasksCompletedId == R.id.radioTasksYes;
        surveyData.put("completedTasks", completedTasks);

        int productivitySatisfiedId = radioGroupProductivity.getCheckedRadioButtonId();
        boolean productivitySatisfied = productivitySatisfiedId == R.id.radioProductivityYes;
        surveyData.put("productivitySatisfied", productivitySatisfied);

        // Physical Health
        int exercisedId = radioGroupExercise.getCheckedRadioButtonId();
        boolean didExercise = exercisedId == R.id.radioExerciseYes;
        surveyData.put("didExercise", didExercise);
        surveyData.put("exerciseDays", spinnerExerciseDays.getSelectedItem().toString());
        surveyData.put("energyLevel", (int) ratingEnergyLevels.getRating());

        int symptomsId = radioGroupPhysicalSymptoms.getCheckedRadioButtonId();
        boolean hadSymptoms = symptomsId == R.id.radioSymptomsYes;
        surveyData.put("hadPhysicalSymptoms", hadSymptoms);
        surveyData.put("symptoms", editTextSymptoms.getText().toString());

        // Add timestamp
        surveyData.put("timestamp", System.currentTimeMillis());

        // Save to Firebase using the DataRepository
        DataRepository.getInstance().saveSurveyData(surveyData);

        Toast.makeText(SurveyActivity.this, "Survey submitted successfully!", Toast.LENGTH_SHORT).show();

        // Re-enable the button after submission
        buttonSubmitSurvey.setEnabled(true);

        // Go back to the main activity
        finish();
    }
}