
public class Cipher{
 
        public static String decrypt(String key, String input) {
                char arrayInput[] = input.toCharArray();
                char arrayKey[] = key.toCharArray();
               
               
                for (int i = 0, keypos = 0; i < arrayInput.length; i++, keypos = (keypos + 1) % arrayKey.length) {
                        arrayInput[i] = (char) (arrayInput[i] + arrayKey[keypos]);
                }
                return new String(arrayInput);
        }
 
        public static String encrypt(String key, String input) {
                char arrayInput[] = input.toCharArray();
                char arrayKey[] = key.toCharArray();
               
               
                for (int i = 0, keypos = 0; i < arrayInput.length; i++, keypos = (keypos + 1) % arrayKey.length) {
                        arrayInput[i] = (char) (arrayInput[i] - arrayKey[keypos]);
                }
                return new String(arrayInput);
        }
}