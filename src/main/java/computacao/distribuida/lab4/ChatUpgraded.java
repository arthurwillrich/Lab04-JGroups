package computacao.distribuida.lab4;

import org.jgroups.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatUpgraded implements Receiver {
    protected JChannel channel;
    protected static final String CLUSTER = "chat";

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    public void receive(Message msg) {
        String line = "[" + msg.getSrc() + "]: " + msg.getObject();
        System.out.println(line);
    }

    private void sendDirectMessage(Address destination, String message) throws Exception {
        Message msg = new ObjectMessage(destination, message);
        channel.send(msg);
    }

    private void listMembers() {
        View view = channel.getView();
        System.out.println("Available members:");
        for (Address member : view.getMembers()) {
            System.out.println("- " + member);
        }
    }

    private void eventLoop() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("> ");
                System.out.flush();
                String line = in.readLine().trim().toLowerCase();

                if (line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                } else if (line.startsWith("/dm")) {
                    String[] tokens = line.split(" ", 3);
                    if (tokens.length == 3) {
                        String recipientName = tokens[1];
                        String message = tokens[2];

                        Address recipientAddress = findAddressByName(recipientName);
                        if (recipientAddress != null) {
                            sendDirectMessage(recipientAddress, "[PM] " + message);
                        } else {
                            System.out.println("Recipient not found.");
                        }
                    } else {
                        System.out.println("Invalid format. Usage: /dm <recipient> <message>");
                    }
                } else if (line.equals("/list")) {
                    listMembers();
                } else {
                    Message msg = new ObjectMessage(null, line);
                    channel.send(msg);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    private Address findAddressByName(String name) {
        View view = channel.getView();
        for (Address member : view.getMembers()) {
            String memberName = member.toString();
            if (memberName.contains(name)) {
                return member;
            }
        }
        return null;
    }

    private void start(String props, String name, boolean nohup) throws Exception {
        channel = new JChannel(props);
        if (name != null)
            channel.name(name);
        channel.setReceiver(this);
        channel.connect(CLUSTER);
        if (!nohup) {
            eventLoop();
            channel.close();
        }
    }

    public static void main(String[] args) throws Exception {
        String props = "udp.xml";
        String name = null;
        boolean nohup = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-props")) {
                props = args[++i];
                continue;
            }
            if (args[i].equals("-name")) {
                name = args[++i];
                continue;
            }
            if (args[i].equals("-nohup")) {
                nohup = true;
                continue;
            }
            help();
            return;
        }

        new ChatUpgraded().start(props, name, nohup);
    }

    protected static void help() {
        System.out.println("Chat [-props XML config] [-name name] [-nohup]");
    }
}
