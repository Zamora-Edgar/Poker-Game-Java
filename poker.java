import java.util.*;

// --- SUPPORTING CLASSES ---

class Card implements Comparable<Card> {
    public final String suit;
    public final String rank;
    public final int value;

    public Card(String suit, String rank, int value) {
        this.suit = suit;
        this.rank = rank;
        this.value = value;
    }

    @Override
    public String toString() { return rank + " of " + suit; }
    
    @Override
    public int compareTo(Card o) { return Integer.compare(this.value, o.value); }
}

class Player {
    String name;
    List<Card> hand = new ArrayList<>();
    int chips;
    boolean isFolded = false;

    public Player(String name, int chips) {
        this.name = name;
        this.chips = chips;
    }
}

// --- MAIN GAME ENGINE ---

public class TexasHoldem {
    static Scanner scanner = new Scanner(System.in);
    static List<Card> deck = new ArrayList<>();
    static List<Card> communityCards = new ArrayList<>();
    static List<Player> players = new ArrayList<>();
    static int pot = 0;

    public static void main(String[] args) {
        System.out.println("--- TEXAS HOLD'EM CONSOLE ENGINE ---");
        System.out.print("Enter number of bots (1-7): ");
        int botCount = scanner.nextInt();
        
        // Setup Players
        players.add(new Player("You", 1000));
        for (int i = 1; i <= botCount; i++) {
            players.add(new Player("Bot " + i, 1000));
        }

        // Game Loop
        while (true) {
            playHand();
            System.out.print("\nPlay another hand? (y/n): ");
            if (!scanner.next().equalsIgnoreCase("y")) break;
        }
    }

    public static void playHand() {
        // 1. Reset & Deal
        initializeDeck();
        communityCards.clear();
        pot = 0;
        for (Player p : players) {
            p.hand.clear();
            p.isFolded = false;
            p.hand.add(deck.remove(0));
            p.hand.add(deck.remove(0));
        }

        // 2. Pre-Flop
        System.out.println("\n--- NEW HAND ---");
        System.out.println("Your Hand: " + players.get(0).hand);
        doBettingRound("Pre-Flop");
        if (checkWinnerByFold()) return;

        // 3. Flop
        deck.remove(0); // Burn
        communityCards.add(deck.remove(0));
        communityCards.add(deck.remove(0));
        communityCards.add(deck.remove(0));
        System.out.println("\n--- FLOP --- " + communityCards);
        doBettingRound("Flop");
        if (checkWinnerByFold()) return;

        // 4. Turn
        deck.remove(0); // Burn
        communityCards.add(deck.remove(0));
        System.out.println("\n--- TURN --- " + communityCards);
        doBettingRound("Turn");
        if (checkWinnerByFold()) return;

        // 5. River
        deck.remove(0); // Burn
        communityCards.add(deck.remove(0));
        System.out.println("\n--- RIVER --- " + communityCards);
        doBettingRound("River");
        if (checkWinnerByFold()) return;

        // 6. Showdown
        determineWinner();
    }

    public static void doBettingRound(String stage) {
        // Simplified Betting: You act, then bots simulate checks/calls
        if (players.get(0).isFolded) return;

        System.out.print(stage + " Action (check/fold/bet): ");
        String action = scanner.next();

        if (action.equalsIgnoreCase("fold")) {
            players.get(0).isFolded = true;
            System.out.println("You folded.");
        } else if (action.equalsIgnoreCase("bet")) {
            System.out.println("You bet 50 chips.");
            pot += 50;
        } else {
            System.out.println("You checked.");
        }
        
        // Simple Bot Logic (Randomly fold weak hands)
        for (int i = 1; i < players.size(); i++) {
            Player bot = players.get(i);
            if (!bot.isFolded) {
                if (Math.random() < 0.1) {
                    bot.isFolded = true;
                    System.out.println(bot.name + " folds.");
                } else {
                    System.out.println(bot.name + " checks/calls.");
                }
            }
        }
    }

    public static boolean checkWinnerByFold() {
        int active = 0;
        Player lastActive = null;
        for (Player p : players) {
            if (!p.isFolded) {
                active++;
                lastActive = p;
            }
        }
        if (active == 1) {
            System.out.println("\nEveryone folded. " + lastActive.name + " wins the pot!");
            return true;
        }
        return false;
    }

    public static void determineWinner() {
        System.out.println("\n--- SHOWDOWN ---");
        Player winner = null;
        int bestScore = -1;

        for (Player p : players) {
            if (p.isFolded) continue;
            
            // Combine hole cards + community
            List<Card> allCards = new ArrayList<>(p.hand);
            allCards.addAll(communityCards);
            
            int score = evaluateHand(allCards);
            System.out.println(p.name + " has " + p.hand + " -> Score: " + score);

            if (score > bestScore) {
                bestScore = score;
                winner = p;
            }
        }
        System.out.println("\nWINNER: " + winner.name + " wins the pot!");
    }

    // Simplified 7-Card Evaluator (Returns integer score)
    // 800=Straight Flush, 700=Quads, 600=Full House, 500=Flush, 400=Straight...
    public static int evaluateHand(List<Card> cards) {
        Collections.sort(cards); // Sort by value
        
        Map<String, Integer> suits = new HashMap<>();
        Map<Integer, Integer> ranks = new HashMap<>();
        
        for (Card c : cards) {
            suits.put(c.suit, suits.getOrDefault(c.suit, 0) + 1);
            ranks.put(c.value, ranks.getOrDefault(c.value, 0) + 1);
        }

        boolean flush = suits.values().stream().anyMatch(count -> count >= 5);
        boolean straight = checkStraight(cards);

        if (flush && straight) return 800; // Straight Flush (Sim)
        if (ranks.containsValue(4)) return 700; // Quads
        if (ranks.containsValue(3) && ranks.containsValue(2)) return 600; // Full House
        if (flush) return 500; // Flush
        if (straight) return 400; // Straight
        if (ranks.containsValue(3)) return 300; // Trips
        
        int pairs = 0;
        for (int count : ranks.values()) if (count == 2) pairs++;
        if (pairs >= 2) return 200; // Two Pair
        if (pairs == 1) return 100; // Pair

        // High Card (return highest value)
        return cards.get(cards.size() - 1).value;
    }

    public static boolean checkStraight(List<Card> cards) {
        int streak = 0;
        // Simplified straight check on sorted unique values
        for (int i = 0; i < cards.size() - 1; i++) {
            if (cards.get(i+1).value == cards.get(i).value + 1) streak++;
            else if (cards.get(i+1).value != cards.get(i).value) streak = 0;
            if (streak >= 4) return true;
        }
        return false;
    }

    public static void initializeDeck() {
        deck.clear();
        String[] s = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] r = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
        int[] v = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

        for (String suit : s) {
            for (int i = 0; i < r.length; i++) {
                deck.add(new Card(suit, r[i], v[i]));
            }
        }
        Collections.shuffle(deck);
    }
}
