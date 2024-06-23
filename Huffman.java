import java.util.*;
import java.io.*;

public class Huffman {
    private static class Node {
        char data;
        int freq;
        String code;
        Node left, right;

        Node() {
            left = right = null;
        }
    }

    private List<Node> arr;
    private String inFileName, outFileName;
    private Node root;

    private static class Compare implements Comparator<Node> {
        @Override
        public int compare(Node l, Node r) {
            return Integer.compare(l.freq, r.freq);
        }
    }

    private PriorityQueue<Node> minHeap;

    public Huffman(String inFileName, String outFileName) {
        this.inFileName = inFileName;
        this.outFileName = outFileName;
        createArr();
    }

    private void createArr() {
        arr = new ArrayList<>(128);
        for (int i = 0; i < 128; i++) {
            arr.add(new Node());
            arr.get(i).data = (char) i;
            arr.get(i).freq = 0;
        }
    }

    private void traverse(Node r, String str) {
        if (r.left == null && r.right == null) {
            r.code = str;
            return;
        }

        traverse(r.left, str + '0');
        traverse(r.right, str + '1');
    }

    private int binToDec(String inStr) {
        int res = 0;
        for (char c : inStr.toCharArray()) {
            res = res * 2 + (c - '0');
        }
        return res;
    }

    private String decToBin(int inNum) {
        StringBuilder temp = new StringBuilder();
        StringBuilder res = new StringBuilder();
        while (inNum > 0) {
            temp.append((char) (inNum % 2 + '0'));
            inNum /= 2;
        }
        res.append("0".repeat(Math.max(0, 8 - temp.length())));
        return res.append(temp.reverse()).toString();
    }

    private void buildTree(char a_code, String path) {
        Node curr = root;
        for (char c : path.toCharArray()) {
            if (c == '0') {
                if (curr.left == null) {
                    curr.left = new Node();
                }
                curr = curr.left;
            } else if (c == '1') {
                if (curr.right == null) {
                    curr.right = new Node();
                }
                curr = curr.right;
            }
        }
        curr.data = a_code;
    }

    private void createMinHeap() {
        try (BufferedReader reader = new BufferedReader(new FileReader(inFileName))) {
            int id;
            while ((id = reader.read()) != -1) {
                arr.get(id).freq++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        minHeap = new PriorityQueue<>(new Compare());
        for (Node node : arr) {
            if (node.freq > 0) {
                minHeap.offer(node);
            }
        }
    }

    private void createTree() {
        PriorityQueue<Node> tempPQ = new PriorityQueue<>(minHeap);
        while (tempPQ.size() != 1) {
            Node left = tempPQ.poll();
            Node right = tempPQ.poll();

            root = new Node();
            root.freq = left.freq + right.freq;
            root.left = left;
            root.right = right;
            tempPQ.offer(root);
        }
    }

    private void createCodes() {
        traverse(root, "");
    }

    private void saveEncodedFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(inFileName));
             BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(outFileName))) {
            
            StringBuilder in = new StringBuilder();
            in.append((char) minHeap.size());

            PriorityQueue<Node> tempPQ = new PriorityQueue<>(minHeap);
            while (!tempPQ.isEmpty()) {
                Node curr = tempPQ.poll();
                in.append(curr.data);

                String s = "0".repeat(127 - curr.code.length()) + "1" + curr.code;
                for (int i = 0; i < 16; i++) {
                    in.append((char) binToDec(s.substring(0, 8)));
                    s = s.substring(8);
                }
            }

            StringBuilder s = new StringBuilder();
            int id;
            while ((id = reader.read()) != -1) {
                s.append(arr.get(id).code);
                while (s.length() > 8) {
                    in.append((char) binToDec(s.substring(0, 8)));
                    s = new StringBuilder(s.substring(8));
                }
            }

            int count = 8 - s.length();
            if (s.length() < 8) {
                s.append("0".repeat(count));
            }
            in.append((char) binToDec(s.toString()));
            in.append((char) count);

            writer.write(in.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDecodedFile() {
        try (BufferedInputStream reader = new BufferedInputStream(new FileInputStream(inFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName))) {
    
            int size = reader.read();
            reader.skip(17 * size);
    
            List<Integer> text = new ArrayList<>();
            int textseg;
            while ((textseg = reader.read()) != -1) {
                text.add(textseg);
            }
            int count0 = text.remove(text.size() - 1);
    
            Node curr = root;
            if (curr == null) {
                throw new IllegalStateException("Huffman tree root is null. Make sure getTree() is called before saveDecodedFile().");
            }
    
            for (int i = 0; i < text.size(); i++) {
                String path = decToBin(text.get(i));
                if (i == text.size() - 1) {
                    path = path.substring(0, 8 - count0);
                }
    
                for (char c : path.toCharArray()) {
                    if (c == '0') {
                        if (curr.left == null) {
                            throw new IllegalStateException("Unexpected null left child in Huffman tree");
                        }
                        curr = curr.left;
                    } else {
                        if (curr.right == null) {
                            throw new IllegalStateException("Unexpected null right child in Huffman tree");
                        }
                        curr = curr.right;
                    }
    
                    if (curr.left == null && curr.right == null) {
                        writer.write(curr.data);
                        curr = root;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getTree() {
        try (BufferedInputStream reader = new BufferedInputStream(new FileInputStream(inFileName))) {
            int size = reader.read();
            root = new Node();

            for (int i = 0; i < size; i++) {
                char aCode = (char) reader.read();
                byte[] hCodeC = new byte[16];
                reader.read(hCodeC);

                StringBuilder hCodeStr = new StringBuilder();
                for (byte b : hCodeC) {
                    hCodeStr.append(decToBin(b & 0xFF));
                }

                int j = 0;
                while (hCodeStr.charAt(j) == '0') {
                    j++;
                }
                hCodeStr = new StringBuilder(hCodeStr.substring(j + 1));

                buildTree(aCode, hCodeStr.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void compress() {
        createMinHeap();
        createTree();
        createCodes();
        saveEncodedFile();
    }

    public void decompress() {
        getTree();
        saveDecodedFile();
    }
}