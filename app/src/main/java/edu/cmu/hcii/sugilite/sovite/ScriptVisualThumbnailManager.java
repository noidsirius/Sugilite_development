package edu.cmu.hcii.sugilite.sovite;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import java.io.File;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 2/20/20
 * @time 8:31 PM
 */

public class ScriptVisualThumbnailManager {
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private Context context;

    public ScriptVisualThumbnailManager(Activity context) {
        this.sugiliteData = (SugiliteData) context.getApplication();
        this.context = context;
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
    }

    public Drawable getVisualThumbnailForScript (SugiliteBlock script, String utterance) {
        //1. get the last available screenshot (recursively expand get_procedure calls)
        File screenshotFile = null;
        try {
            screenshotFile = getLastAvailableScreenshotInSubsequentScript(script);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (screenshotFile != null) {
            // there is screenshot available, get the Drawable from File
            String path = screenshotFile.getAbsolutePath();
            Drawable drawable = Drawable.createFromPath(path);
            return drawable;
        }

        return null;
    }

    private File getLastAvailableScreenshotInSubsequentScript (SugiliteBlock script) throws Exception {
        if (script == null) {
            return null;
        }
        File currentLastAvailableScreenshotInSubsequentScript = null;
        if (script.getScreenshot() != null) {
            // update the screenshot if available
            currentLastAvailableScreenshotInSubsequentScript = script.getScreenshot();
        }
        if (script instanceof SugiliteConditionBlock) {
            // handle condition block
            File currentLastAvailableScreenshotInThenBlock = getLastAvailableScreenshotInSubsequentScript(((SugiliteConditionBlock) script).getThenBlock());
            if (currentLastAvailableScreenshotInThenBlock != null) {
                currentLastAvailableScreenshotInSubsequentScript = currentLastAvailableScreenshotInThenBlock;
            }
        }
        if (script instanceof SugiliteOperationBlock && ((SugiliteOperationBlock) script).getOperation() instanceof SugiliteGetProcedureOperation) {
            // handle get_procedure calls
            String subScriptName = ((SugiliteGetProcedureOperation) ((SugiliteOperationBlock) script).getOperation()).evaluate(sugiliteData);
            SugiliteStartingBlock subScript = sugiliteScriptDao.read(subScriptName);
            File currentLastAvailableScreenshotInSubBlock = getLastAvailableScreenshotInSubsequentScript(subScript);
            if (currentLastAvailableScreenshotInSubBlock != null) {
                currentLastAvailableScreenshotInSubsequentScript = currentLastAvailableScreenshotInSubBlock;
            }

        }
        File currentLastAvailableScreenshotInNextBlock = getLastAvailableScreenshotInSubsequentScript(script.getNextBlock());
        if (currentLastAvailableScreenshotInNextBlock != null) {
            currentLastAvailableScreenshotInSubsequentScript = currentLastAvailableScreenshotInNextBlock;
        }
        return currentLastAvailableScreenshotInSubsequentScript;
    }
}