public class Decode {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Failed to detect Files");
            System.exit(1);
        }

        Huffman f = new Huffman(args[0], args[1]);
        f.decompress();
        System.out.println("Decompressed successfully");
    }
}