

import java.io.*;
import java.net.*;
import java.util.*;

class RpsServer {
    private static final int PORT = 5000;
    private static final List<String> CHOICES = Arrays.asList("rock", "paper", "scissors");
    private static String randomChoice(Random r){
        return CHOICES.get(r.nextInt(CHOICES.size()));
    }
    private static String result(String client, String server){
        if (client.equals(server)) return "Draw";
        boolean clientWins =
            (client.equals("rock") && server.equals("scissors")) ||
            (client.equals("paper") && server.equals("rock")) ||
            (client.equals("scissors") && server.equals("paper"));
        return clientWins ? "You win" : "You lose";
    }
    public static void start() {
        System.out.println("[RPS-Server] Starting on port " + PORT + " …");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[RPS-Server] Waiting for a client…");
            try (Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                System.out.println("[RPS-Server] Client connected: " + socket.getRemoteSocketAddress());
                out.println("Welcome to Rock-Paper-Scissors! Type rock/paper/scissors or quit.");

                String line;
                Random r = new Random();
                while ((line = in.readLine()) != null) {
                    String msg = line.trim().toLowerCase();
                    if (msg.equals("quit")) {
                        out.println("Goodbye!");
                        System.out.println("[RPS-Server] Client requested quit. Closing.");
                        break;
                    }
                    if (!CHOICES.contains(msg)) {
                        out.println("Invalid input. Use rock, paper, scissors, or quit.");
                        continue;
                    }
                    String serverPick = randomChoice(r);
                    String outcome = result(msg, serverPick);
                    out.println("Server chose: " + serverPick + ". Result: " + outcome);
                    System.out.printf("[RPS-Server] Client=%s | Server=%s | %s%n", msg, serverPick, outcome);
                }
            }
        } catch (IOException e) {
            System.err.println("[RPS-Server] Error: " + e.getMessage());
        }
        System.out.println("[RPS-Server] Stopped.");
    }
}


class RpsClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    public static void start() {
        System.out.println("[RPS-Client] Connecting to " + HOST + ":" + PORT + " …");
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            String greeting = in.readLine();
            if (greeting != null) System.out.println(greeting);

            while (true) {
                System.out.print("Enter choice (rock/paper/scissors) or quit: ");
                String choice = scanner.nextLine();
                out.println(choice);

                String reply = in.readLine();
                if (reply == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }
                System.out.println(reply);
                if (choice.trim().equalsIgnoreCase("quit")) break;
            }
        } catch (IOException e) {
            System.err.println("[RPS-Client] Error: " + e.getMessage());
        }
        System.out.println("[RPS-Client] Bye.");
    }
}


class UdpGuessServer {
    private static final int PORT = 6000;
    private static final int MIN = 1, MAX = 10;

    public static void start() {
        System.out.println("[UDP-Server] Starting on port " + PORT + " …");
        Random random = new Random();
        int target = MIN + random.nextInt(MAX - MIN + 1);
        System.out.println("[UDP-Server] Target number is " + target + " (hidden from client)");

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[1024];
            boolean running = true;

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                String response;
                if (received.equalsIgnoreCase("quit")) {
                    response = "Goodbye!";
                    running = false;
                } else {
                    try {
                        int guess = Integer.parseInt(received);
                        if (guess < MIN || guess > MAX) {
                            response = "Please guess between " + MIN + " and " + MAX + ".";
                        } else if (guess < target) {
                            response = "Too low";
                        } else if (guess > target) {
                            response = "Too high";
                        } else {
                            response = "Correct! You win!";
                            running = false;
                        }
                    } catch (NumberFormatException nfe) {
                        response = "Send a number (" + MIN + "-" + MAX + ") or 'quit'.";
                    }
                }

                byte[] outBytes = response.getBytes();
                DatagramPacket outPacket = new DatagramPacket(outBytes, outBytes.length, packet.getAddress(), packet.getPort());
                socket.send(outPacket);

                System.out.printf("[UDP-Server] From %s:%d | msg='%s' -> reply='%s'%n", packet.getAddress().getHostAddress(), packet.getPort(), received, response);
            }
        } catch (IOException e) {
            System.err.println("[UDP-Server] Error: " + e.getMessage());
        }
        System.out.println("[UDP-Server] Stopped.");
    }
}

class UdpGuessClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 6000;

    public static void start() {
        System.out.println("[UDP-Client] Talking to " + HOST + ":" + PORT + " …\nType a number between 1-10 or 'quit'.");

        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.print("Your guess: ");
                String msg = scanner.nextLine().trim();

                byte[] bytes = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(HOST), PORT);
                socket.send(packet);

                byte[] buffer = new byte[1024];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                socket.receive(reply);

                String response = new String(reply.getData(), 0, reply.getLength());
                System.out.println("Server: " + response);

                if (msg.equalsIgnoreCase("quit") || response.startsWith("Correct!")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[UDP-Client] Error: " + e.getMessage());
        }
        System.out.println("[UDP-Client] Bye.");
    }
}


public class GamesMain {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Choose mode:\n1. RPS Server\n2. RPS Client\n3. UDP Guess Server\n4. UDP Guess Client");
        int choice = sc.nextInt();
        switch (choice) {
            case 1 -> RpsServer.start();
            case 2 -> RpsClient.start();
            case 3 -> UdpGuessServer.start();
            case 4 -> UdpGuessClient.start();
            default -> System.out.println("Invalid option");
        }
    }
}

