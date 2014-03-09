package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_MAXNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.INCORRECT_MINNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.INCORRECT_OPTIONSAREBINARY;
import static nxt.http.JSONResponses.INCORRECT_POLL_DESCRIPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_POLL_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_POLL_OPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_DESCRIPTION;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_MAXNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.MISSING_MINNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.MISSING_NAME;
import static nxt.http.JSONResponses.MISSING_OPTIONSAREBINARY;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class CreatePoll extends CreateTransaction {

    static final CreatePoll instance = new CreatePoll();

    private CreatePoll() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String nameValue = req.getParameter("name");
        String descriptionValue = req.getParameter("description");
        String minNumberOfOptionsValue = req.getParameter("minNumberOfOptions");
        String maxNumberOfOptionsValue = req.getParameter("maxNumberOfOptions");
        String optionsAreBinaryValue = req.getParameter("optionsAreBinary");

        if (nameValue == null) {
            return MISSING_NAME;
        } else if (descriptionValue == null) {
            return MISSING_DESCRIPTION;
        } else if (minNumberOfOptionsValue == null) {
            return MISSING_MINNUMBEROFOPTIONS;
        } else if (maxNumberOfOptionsValue == null) {
            return MISSING_MAXNUMBEROFOPTIONS;
        } else if (optionsAreBinaryValue == null) {
            return MISSING_OPTIONSAREBINARY;
        }

        if (nameValue.length() > Nxt.MAX_POLL_NAME_LENGTH) {
            return INCORRECT_POLL_NAME_LENGTH;
        }

        if (descriptionValue.length() > Nxt.MAX_POLL_DESCRIPTION_LENGTH) {
            return INCORRECT_POLL_DESCRIPTION_LENGTH;
        }

        List<String> options = new ArrayList<>();
        while (options.size() < 100) {
            String optionValue = req.getParameter("option" + options.size());
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Nxt.MAX_POLL_OPTION_LENGTH) {
                return INCORRECT_POLL_OPTION_LENGTH;
            }
            options.add(optionValue.trim());
        }

        byte minNumberOfOptions;
        try {
            minNumberOfOptions = Byte.parseByte(minNumberOfOptionsValue);
        } catch (NumberFormatException e) {
            return INCORRECT_MINNUMBEROFOPTIONS;
        }

        byte maxNumberOfOptions;
        try {
            maxNumberOfOptions = Byte.parseByte(maxNumberOfOptionsValue);
        } catch (NumberFormatException e) {
            return INCORRECT_MAXNUMBEROFOPTIONS;
        }

        boolean optionsAreBinary;
        try {
            optionsAreBinary = Boolean.parseBoolean(optionsAreBinaryValue);
        } catch (NumberFormatException e) {
            return INCORRECT_OPTIONSAREBINARY;
        }

        Account account = getAccount(req);
        if (account == null) {
            return NOT_ENOUGH_FUNDS;
        }

        Attachment attachment = new Attachment.MessagingPollCreation(nameValue.trim(), descriptionValue.trim(),
                options.toArray(new String[options.size()]), minNumberOfOptions, maxNumberOfOptions, optionsAreBinary);
        return createTransaction(req, account, attachment);

    }

}
