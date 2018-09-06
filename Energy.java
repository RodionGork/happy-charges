import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Energy {
    
    static final int MAX_LINK = 3;
    static final double ENERGY_COEFF = 1389.38757;
    
    Atom[] atoms;
    
    static class Atom {
        
        int id;
        
        double x, y, z;
        
        double charge;
        
        List<Atom> linked = new ArrayList<>();
        Map<Atom, Integer> closest;
        
        Atom(int id, double x, double y, double z) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        double dist(Atom other) {
            return Math.hypot(other.x - x, Math.hypot(other.y - y, other.z - z));
        }
        
        double energy(Atom other) {
            var v = charge * other.charge / dist(other);
            var f = closest.get(other);
            if (f == null) {
                return v;
            } else if (f < MAX_LINK) {
                return 0;
            } else {
                return v / 2;
            }
        }
    }
    
    void load() {
        try (var r = new BufferedReader(new FileReader("atoms.txt"))) {
            var input = Arrays.stream(r.readLine().split("\\s+"))
                    .map(s -> Double.parseDouble(s)).collect(Collectors.toList());
            atoms = new Atom[input.size() / 3];
            for (int i = 0; i < input.size(); i += 3) {
                atoms[i / 3] = new Atom(i / 3, input.get(i), input.get(i + 1), input.get(i + 2));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (var r = new BufferedReader(new FileReader("bonds.txt"))) {
            var input = Arrays.stream(r.readLine().split("\\s+"))
                    .map(s -> Integer.parseInt(s)).collect(Collectors.toList());
            for (int i = 0; i < input.size(); i += 2) {
                var a1 = atoms[input.get(i)];
                var a2 = atoms[input.get(i + 1)];
                a1.linked.add(a2);
                a2.linked.add(a1);
                
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (var r = new BufferedReader(new FileReader("charges.txt"))) {
            var input = r.readLine().split("\\s+");
            for (int i = 0; i < atoms.length; i++) {
                atoms[i].charge = Double.parseDouble(input[i]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    void run() {
        class Calculator implements Runnable {
            int offs, step;
            double energy;
            Calculator(int offs, int step) {
                this.offs = offs;
                this.step = step;
            }
            public void run() {
                energy = 0;
                for (int i = 1 + offs; i < atoms.length; i += step) {
                    for (int j = 0; j < i; j++) {
                        energy += atoms[j].energy(atoms[i]);
                    }
                }
            }
        }
        double energy = 0;
        for (Atom a : atoms) {
            a.closest = findClosest(a, MAX_LINK);
        }
        int tn = Runtime.getRuntime().availableProcessors();
        var calcs = new Calculator[tn];
        var threads = new Thread[tn];
        for (int t = 0; t < tn; t++) {
            calcs[t] = new Calculator(t + 1, tn);
            threads[t] = new Thread(calcs[t]);
            threads[t].start();
        }
        for (int t = 0; t < tn; t++) {
            try {
                threads[t].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            energy += calcs[t].energy;
        }
        energy *= ENERGY_COEFF;
        System.out.println("Result: " + energy);
    }
    
    Map<Atom, Integer> findClosest(Atom center, int dist) {
        var result = new HashMap<Atom, Integer>();
        var temp = new ArrayDeque<Atom>();
        result.put(center, 0);
        temp.add(center);
        while (!temp.isEmpty()) {
            var next = temp.removeFirst();
            int nextLevel = result.get(next) + 1;
            if (nextLevel > dist) {
                break;
            }
            for (Atom a : next.linked) {
                if (!result.containsKey(a)) {
                    result.put(a, nextLevel);
                    temp.add(a);
                }
            }
        }
        result.remove(center);
        return result;
    }
    
    void linkedComponents() {
        var set = new HashSet<>(Arrays.asList(atoms));
        while (!set.isEmpty()) {
            var head = set.iterator().next();
            var subSet = component(head);
            set.removeAll(subSet);
            System.out.println(head.id + ": " + subSet.size());
        }
    }
    
    Set<Atom> component(Atom initial) {
        var result = new HashSet<Atom>();
        var temp = new ArrayDeque<Atom>();
        temp.add(initial);
        result.add(initial);
        while (!temp.isEmpty()) {
            var next = temp.removeLast();
            for (Atom a : next.linked) {
                if (!result.contains(a)) {
                    result.add(a);
                    temp.add(a);
                }
            }
        }
        return result;
    }
    
    void printLinks() {
        for (Atom a : atoms) {
            System.out.print(a.id + ":");
            for (Atom b : a.linked) {
                System.out.print(" " + b.id);
            }
            System.out.println();
        }
    }
    
    public static void main(String... args) {
        var e = new Energy();
        e.load();
        e.run();
    }
    
}

