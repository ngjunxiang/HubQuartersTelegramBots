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

public class HubQuartersMgmtBot extends TelegramLongPollingBot {
    private int totalCapacity = 80;
    private boolean isFirstTime = true;
    private List<Long> subscribedChatIds = new CopyOnWriteArrayList<>();

    public void onUpdateReceived(Update update) {
        if (isFirstTime) {
            pushNotifications();
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
                welcomeMsg.setText("Welcome to *SCAPE HubQuarters CMS for Management!");
                ReplyKeyboard markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = ((InlineKeyboardMarkup) markup).getKeyboard();
                keyboard.add(new ArrayList<InlineKeyboardButton>());
                keyboard.get(0).add(new InlineKeyboardButton().setText("Show Occupancy Rate").setCallbackData("/showoccupancyrate"));

                synchronized (subscribedChatIds) {
                    if (subscribedChatIds.contains(update.getMessage().getChatId())) {
                        keyboard.get(0).add(new InlineKeyboardButton().setText("Unsubscribe from Updates").setCallbackData("/unsubscribe"));
                    } else {
                        keyboard.get(0).add(new InlineKeyboardButton().setText("Subscribe to Updates").setCallbackData("/subscribe"));
                    }
                }

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
                        synchronized (subscribedChatIds) {
                            if (subscribedChatIds.contains(update.getCallbackQuery().getMessage().getChatId())) {
                                keyboard.get(0).add(new InlineKeyboardButton().setText("Unsubscribe from Updates").setCallbackData("/unsubscribe"));
                            } else {
                                keyboard.get(0).add(new InlineKeyboardButton().setText("Subscribe to Updates").setCallbackData("/subscribe"));
                            }
                        }

                        message.setReplyMarkup(markup);
                    } else {
                        message.setText(response.get(0));
                    }

                    sendMessage(message);
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (reply.contains("/subscribe")) {
                synchronized (subscribedChatIds) {
                    if (!subscribedChatIds.contains(update.getCallbackQuery().getMessage().getChatId())) {
                        subscribedChatIds.add(update.getCallbackQuery().getMessage().getChatId());
                    }
                }

                message.setText("You are now subscribed. You will be notified should there be any alerts.");
                sendMessage(message);
            } else if (reply.contains("/unsubscribe")) {
                synchronized (subscribedChatIds) {
                    if (subscribedChatIds.contains(update.getCallbackQuery().getMessage().getChatId())) {
                        subscribedChatIds.remove(update.getCallbackQuery().getMessage().getChatId());
                    }
                }

                message.enableMarkdown(false);
                message.setText("You are now unsubscribed. Thank you for using *SCAPE HubQuarters CMS!");
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

    public void pushNotifications() {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Thread notifications = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        List<String> response = OccupancyDAO.retrieveData();

                        if (response.size() == 2) {
                            String timestamp = response.get(1);
                            Date lastImageTime = dateFormatter.parse(timestamp);
                            Date currentTime = new Date();
                            System.out.println(lastImageTime.toString());
                            System.out.println(currentTime.toString());
                            long difference = (currentTime.getTime() - lastImageTime.getTime()) / 1000;
                            if (difference > 600) {
                                System.out.println("Alert");
                                synchronized (subscribedChatIds) {
                                    for (long chatId : subscribedChatIds) {
                                        SendMessage alertMessage = new SendMessage();
                                        alertMessage.enableMarkdown(true);
                                        alertMessage.setChatId(chatId);
                                        alertMessage.setText("*ALERT*");
                                        sendMessage(alertMessage);
                                        SendMessage message = new SendMessage();
                                        message.setChatId(chatId);
                                        message.setText(String.format("The last update received was at %s. Please check up on the Raspberry Pi.", timestamp));
                                        sendMessage(message);
                                    }
                                }
                            }

                            System.out.println(String.format("Last checked at %s", dateFormatter.format(currentTime)));
                            System.out.println("Difference: " + difference);
                        } else {
                            synchronized (subscribedChatIds) {
                                for (long chatId : subscribedChatIds) {
                                    SendMessage message = new SendMessage();
                                    message.setChatId(chatId);
                                    message.setText("Something went wrong with the CMS API.");
                                    sendMessage(message);
                                }
                            }
                        }
//                        Thread.sleep(1000);
                        Thread.sleep(600000);
                    } catch (IOException | URISyntaxException | ParseException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        notifications.start();
    }

    public String getBotUsername() {
        return "HubQuartersMgmtBot";
    }

    public String getBotToken() {
        return "880104369:AAEs7mI_VKo5vDj_oHWeZCkL1ZkHyLx9d2s";
    }
}
