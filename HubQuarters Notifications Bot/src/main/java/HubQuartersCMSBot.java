import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HubQuartersCMSBot extends TelegramLongPollingBot {
    private int totalCapacity = 80;
    private boolean isFirstTime = true;
    private List<Long> subscribedChatIds = new CopyOnWriteArrayList<>();

    public void onUpdateReceived(Update update) {
        if (isFirstTime) {
            checkAvailability();
            isFirstTime = false;
        }

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
                keyboard.add(new ArrayList<InlineKeyboardButton>());
                keyboard.get(1).add(new InlineKeyboardButton().setText("Request for Seat").setCallbackData("/request"));
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
                        keyboard.get(0).add(new InlineKeyboardButton().setText("Request for Seat").setCallbackData("/request"));
                        keyboard.add(new ArrayList<InlineKeyboardButton>());
                        keyboard.get(1).add(new InlineKeyboardButton().setText("Update Me").setCallbackData("/showoccupancyrate"));
                        keyboard.get(1).add(new InlineKeyboardButton().setText("Done").setCallbackData("/done"));
                        message.setReplyMarkup(markup);
                    } else {
                        message.setText(response.get(0));
                    }

                    sendMessage(message);
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (reply.contains("/request")) {
                synchronized (subscribedChatIds) {
                    if (!subscribedChatIds.contains(update.getCallbackQuery().getMessage().getChatId())) {
                        subscribedChatIds.add(update.getCallbackQuery().getMessage().getChatId());
                    }
                }
                message.enableMarkdown(false);
                message.setText("We will notify you when a seat is available!");
                sendMessage(message);
            } else if (reply.contains("/done")) {
                message.enableMarkdown(false);
                message.setText("Thank you for using *SCAPE HubQuarters CMS!");
                sendMessage(message);
            }
        }
    }

    public void checkAvailability() {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Thread availability = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (subscribedChatIds) {
                        if (subscribedChatIds.size() > 0) {
                            try {
                                List<String> response = OccupancyDAO.retrieveData();
                                System.out.println(response.size());
                                if (response.size() == 2) {
                                    System.out.println("update");
                                    String timestamp = response.get(1);
                                    Date lastImageTime = dateFormatter.parse(timestamp);
                                    Date currentTime = new Date();
                                    long difference = (currentTime.getTime() - lastImageTime.getTime()) / 1000;
                                    if (difference < 600) {
                                        int numPeople = Integer.parseInt(response.get(0));
                                        System.out.println(numPeople);
                                        if (numPeople < 80) {
                                            for (long chatId : subscribedChatIds) {
                                                SendMessage updateMessage = new SendMessage();
                                                updateMessage.enableMarkdown(true);
                                                updateMessage.setChatId(chatId);
                                                updateMessage.setText("*UPDATE*\nThere are " + (80 - numPeople) + " seats available now.");
                                                sendMessage(updateMessage);
                                            }
                                            subscribedChatIds = new ArrayList<>();
                                        }
                                    } else {
                                        System.out.println("CMS error");
                                        System.out.println(response.get(0));
                                    }

                                    System.out.println(String.format("Last checked at %s", dateFormatter.format(currentTime)));
                                    System.out.println("Difference: " + difference);
                                }
                            } catch (IOException | URISyntaxException | ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        availability.start();
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
    }

    public String getBotUsername() {
        return "HubQuartersCMSBot";
    }

    public String getBotToken() {
        return "628348158:AAFU45gofwn6yCODS__SWbmH5pxnnrzbAwI";
    }
}
