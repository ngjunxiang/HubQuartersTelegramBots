package hubquarters.cms.tenants;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class HubQuartersCMSBot extends TelegramLongPollingBot {
    private int totalCapacity = 80;

    public void onUpdateReceived(Update update) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String command = update.getMessage().getText();
            message.setChatId(update.getMessage().getChatId());

            if (command.contains("/start")) {
                message.setText("Hello, " + update.getMessage().getFrom().getFirstName() + "!");

                SendMessage welcomeMsg = new SendMessage();
                welcomeMsg.setChatId(update.getMessage().getChatId());
                welcomeMsg.setText("Welcome to *SCAPE HubQuarters CMS!");
                ReplyKeyboard markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = ((InlineKeyboardMarkup) markup).getKeyboard();
                keyboard.add(new ArrayList<InlineKeyboardButton>());
                keyboard.get(0).add(new InlineKeyboardButton().setText("Show Occupancy Rate").setCallbackData("/showoccupancyrate"));
                welcomeMsg.setReplyMarkup(markup);

                sendMessage(message);
                sendMessage(welcomeMsg);
            }
        } else if (update.hasCallbackQuery()) {
            String reply = update.getCallbackQuery().getData();
            message.setChatId(update.getCallbackQuery().getMessage().getChatId());

            if (reply.contains("/showoccupancyrate")) {
                // retrieve from db
                try {
                    List<String> response = OccupancyDAO.retrieveData();

                    if (response.size() == 2) {
                        int numPeople = Integer.parseInt(response.get(0));
                        double occupancyRate = (double) numPeople / totalCapacity * 100;

                        String timestamp = response.get(1);

                        // send back to user
                        message.setText(String.format("Occupancy rate at HubQuarters is currently at: *%s* with *%s* people. Last updated at *%s*.",
                                String.format("%.2f", occupancyRate) + "%", String.valueOf(numPeople), timestamp));

                        ReplyKeyboard markup = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> keyboard = ((InlineKeyboardMarkup) markup).getKeyboard();
                        keyboard.add(new ArrayList<InlineKeyboardButton>());
                        keyboard.get(0).add(new InlineKeyboardButton().setText("Update Me").setCallbackData("/showoccupancyrate"));
                        keyboard.get(0).add(new InlineKeyboardButton().setText("Done").setCallbackData("/done"));
                        message.setReplyMarkup(markup);
                    } else {
                        message.setText(response.get(0));
                    }

                    sendMessage(message);
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (reply.contains("/done")) {
                message.enableMarkdown(false);
                message.setText("Thank you for using *SCAPE HubQuarters CMS!");
                sendMessage(message);
            }
        }
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
    }

    public String getBotUsername() {
        return "hubquarters.cms.tenants.HubQuartersCMSBot";
    }

    public String getBotToken() {
        return "628348158:AAFU45gofwn6yCODS__SWbmH5pxnnrzbAwI";
    }
}
