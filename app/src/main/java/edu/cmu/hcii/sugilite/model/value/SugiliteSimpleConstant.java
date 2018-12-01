package edu.cmu.hcii.sugilite.model.value;

import java.text.NumberFormat;
import java.text.ParseException;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 11/14/18
 * @time 12:54 AM
 */
public class SugiliteSimpleConstant<T> implements SugiliteValue<T> {
    private T value;
    private String unit;

    public SugiliteSimpleConstant(T value) {
        this.value = value;
    }

    public SugiliteSimpleConstant(T value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public T evaluate() {
        return value;
    }

    @Override
    public String toString() {
        if (value != null) {
            if (isNumeric(value.toString())) {
                //when it's a number
                try {
                    if (unit == null) {
                        return "(number " + NumberFormat.getInstance().parse(value.toString()).toString() + ")";
                    } else {
                        return "(number " + NumberFormat.getInstance().parse(value.toString()).toString() + " " + unit + ")";
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    return "(string " + addQuoteToTokenIfNeeded(value.toString()) + ")";
                }
            } else {
                //when it's a string
                return "(string " + addQuoteToTokenIfNeeded(value.toString()) + ")";
            }
        }
        return super.toString();
    }

    private static boolean isNumeric(String str) {
        try {
            Number d = NumberFormat.getInstance().parse(str);
        } catch (ParseException nfe) {
            return false;
        }
        return true;
    }

}