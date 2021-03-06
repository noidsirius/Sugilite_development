package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResponse;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 12/10/17
 * @time 4:17 AM
 */
public class OverlayChosenPopupDialog implements SugiliteVerbalInstructionHTTPQueryInterface {
    //this dialog should allow the user to confirm the currently selected query, or switch to a different query
    private Context context;
    private AlertDialog dialog;
    private VerbalInstructionOverlayManager overlayManager;
    private VerbalInstructionRecordingManager verbalInstructionRecordingManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteVerbalInstructionHTTPQueryManager httpQueryManager;
    private OverlayChosenPopupDialog overlayChosenPopupDialog;

    public OverlayChosenPopupDialog(Context context, VerbalInstructionOverlayManager overlayManager, Node node, VerbalInstructionServerResults.VerbalInstructionResult chosenResult, List<VerbalInstructionServerResults.VerbalInstructionResult> allResults, SerializableUISnapshot serializableUISnapshot, String utterance,  SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        this.overlayManager = overlayManager;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.verbalInstructionRecordingManager = new VerbalInstructionRecordingManager(context, sugiliteData, sharedPreferences);
        this.httpQueryManager = SugiliteVerbalInstructionHTTPQueryManager.getInstance(context);
        this.overlayChosenPopupDialog = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Const.appNameUpperCase + " Verbal Instruction");

        List<String> operationList = new ArrayList<>();

        //fill in the options
        operationList.add("Confirm parse: " + chosenResult.getFormula());
        operationList.add("Choose a different parse");
        operationList.add("Remove overlays");
        String[] operations = new String[operationList.size()];
        operations = operationList.toArray(operations);
        final String[] operationClone = operations.clone();


        builder.setItems(operationClone, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        //confirm parse
                        //confirm parse, send the result back to the server
                        PumiceDemonstrationUtil.showSugiliteToast("Confirmed parse: " + chosenResult.getFormula(), Toast.LENGTH_SHORT);
                        VerbalInstructionServerResponse response = new VerbalInstructionServerResponse(chosenResult.getFormula(), chosenResult.getId());

                        try {
                            httpQueryManager.sendResponseRequestOnASeparateThread(response, httpQueryManager.getSemanticParsingServerUrl(), overlayChosenPopupDialog);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        overlayManager.removeOverlays();

                        //TODO: if in the recording mode, add the step to recording
                        if (sharedPreferences.getBoolean("recording_in_process", false)) {
                            //if recording is in process
                            verbalInstructionRecordingManager.addToRecording(chosenResult, node, serializableUISnapshot, utterance);

                        }
                        break;
                    case 1:
                        //choose a different parse
                        DifferentParseChooseDialog differentParseChooseDialog = new DifferentParseChooseDialog(context, overlayManager, allResults, serializableUISnapshot, utterance);
                        differentParseChooseDialog.show();
                        break;
                    case 2:
                        //remove overlays
                        overlayManager.removeOverlays();
                        break;
                }
            }
        });
        dialog = builder.create();
    }

    public void show(){
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        dialog.show();
    }


    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        //do nothing
    }

}
