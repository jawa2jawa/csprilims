import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Stack;

public class AcaciaArkJSONParser {
	File jsonFile = null;
	Stack<String> keyStack = new Stack<String>();
	Stack<Integer> indexStack = new Stack<Integer>();
	Stack<Character> operatorStack = new Stack<Character>();
	LinkedHashMap<String, String> dataBase = new LinkedHashMap<String, String>();
	ArrayList<String> elements = new ArrayList<String>();
	char startChars[] = { '[', '{' }, previousClosed = ' ';
	HashMap<Character, Character> endChMap = new HashMap<Character, Character>();
	String previousData = "", nextKey = "";
	Integer nextIndex = 0;

	public AcaciaArkJSONParser(String fileNmae) {
		jsonFile = new File(fileNmae);
		endChMap.put('{', '}');
		endChMap.put('[', ']');
		keyStack.push("");
		parse();
		System.out.println("**********************DB*********************");
		printDataBase();
	}

	private void printDataBase() {
		Set set = dataBase.keySet();
		Iterator iterator = set.iterator();
		String key = "", val = "";
		while (iterator.hasNext()) {
			key = (String) iterator.next();
			val = dataBase.get(key);
			System.out.println(key + "=" + val);
		}

	}

	private void parse() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile)));
			String line = null;
			StringBuilder sb = new StringBuilder();
			try {
				while ((line = br.readLine()) != null) {
					System.out.println(line);
					sb.append(line);
				}
				processData(sb.toString());
			} catch (IOException e) {
// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void processData(String jsonData) {
		String jsonFragment = previousData + jsonData;
		jsonFragment = jsonFragment.trim();
		int len = jsonFragment.length(), previousIndex = 0;
		StringBuilder sb = new StringBuilder();

		String jsonText = "", data[] = null, tos = "", key = "";
		int min = Integer.MAX_VALUE, i = 0;
		int openedCurlybracesIndex = 0, closedCurlybracesIndex = 0, openedBracketsIndex = 0, closedBracketIndex = 0;
		for (; i < len; i++) {
			/*
			 * jsonFragment starts with { or [
			 */
			if (startChars[0] == jsonFragment.charAt(i) || startChars[1] == jsonFragment.charAt(i)) {
				jsonText = jsonFragment.substring(previousIndex, i);
				jsonText = jsonText.trim();
				if (jsonText.length() == 0) {
					jsonText = "$";
				}
				elements = getStringAsTokens(jsonText);
				if (jsonText.endsWith(":")) {
					nextKey = elements.remove(elements.size() - 1);
				}
				if (!indexStack.isEmpty()) {
					nextIndex = indexStack.peek();
				}
				if (elements.size() == 0) {
					elements.add("$");
				}
				if (!operatorStack.isEmpty() && operatorStack.peek() == '[') {
					updateDBwithArray(elements, keyStack.peek(), nextIndex);
				}
				if (!operatorStack.isEmpty() && operatorStack.peek() == '{') {
					updateDBwithMap(elements, keyStack.peek());
				}
				previousIndex = i + 1;
				jsonText = jsonFragment.substring(previousIndex);
				closedCurlybracesIndex = jsonText.indexOf(endChMap.get(startChars[1]));
				closedBracketIndex = jsonText.indexOf(endChMap.get(startChars[0]));
				openedCurlybracesIndex = jsonText.indexOf(startChars[1]);
				openedBracketsIndex = jsonText.indexOf(startChars[0]);
				if (!indexStack.isEmpty() && !keyStack.isEmpty() && keyStack.peek().trim().length() > 0
						&& !operatorStack.isEmpty() && (operatorStack.peek() == '[')) {
					if (keyStack.peek().trim().endsWith(".")) {
						if (nextKey.trim().startsWith(".")) {
							keyStack.push(keyStack.peek() + "[" + indexStack.peek() + "]" + nextKey);
						} else {
							keyStack.push(keyStack.peek() + "[" + indexStack.peek() + "]." + nextKey);
						}
					} else {
						if (nextKey.trim().startsWith(".")) {
							keyStack.push(keyStack.peek() + ".[" + indexStack.peek() + "]" + nextKey);
						} else {
							keyStack.push(keyStack.peek() + ".[" + indexStack.peek() + "]." + nextKey);
						}
					}
				} else {
					if (keyStack.peek().trim().endsWith(".") || nextKey.trim().startsWith(".")) {
						keyStack.push(keyStack.peek() + nextKey);
					} else {
						keyStack.push(keyStack.peek() + "." + nextKey);
					}
				}
				operatorStack.push(jsonFragment.charAt(i));
				nextKey = "";
				if (jsonFragment.charAt(i) == '[') {
					indexStack.push(0);
				}
				if (closedCurlybracesIndex != -1 || closedBracketIndex != -1 || openedCurlybracesIndex != -1
						|| openedBracketsIndex != -1) {
					min = getMinof(openedCurlybracesIndex, closedCurlybracesIndex, openedBracketsIndex,
							closedBracketIndex);
					i += min;
				}
				continue;
			}

			/*
			 * jsonFragment starts with } or ]
			 */
			if ('}' == jsonFragment.charAt(i) || ']' == jsonFragment.charAt(i)) {
				jsonText = jsonFragment.substring(previousIndex, i);
				jsonText = jsonText.trim();
				if (jsonText.length() == 0) {
					jsonText = "$";
				}
				elements = getStringAsTokens(jsonText);
				if (!operatorStack.isEmpty() && operatorStack.peek() == '[' && !keyStack.isEmpty()
						&& !indexStack.isEmpty()) {
					updateDBwithArray(elements, keyStack.peek(), indexStack.peek());
					if (elements.size() == 1 && elements.get(0) == "$" && indexStack.peek() == 0) {
						updateDataBase(keyStack.peek(), "Undefined");
					}
					keyStack.pop();
					indexStack.pop();
				}
				if (!operatorStack.isEmpty() && operatorStack.peek() == '{' && !keyStack.isEmpty()) {
					updateDBwithMap(elements, keyStack.peek());
					keyStack.pop();
				}
				operatorStack.pop();
				if (!operatorStack.isEmpty() && operatorStack.peek() == '[' && !indexStack.isEmpty()) {
					nextIndex = indexStack.pop();
					nextIndex++;
					indexStack.push(nextIndex);
				}
				previousIndex = i + 1;
				jsonText = jsonFragment.substring(previousIndex);
				closedCurlybracesIndex = jsonText.indexOf(endChMap.get(startChars[1]));
				closedBracketIndex = jsonText.indexOf(endChMap.get(startChars[0]));
				openedCurlybracesIndex = jsonText.indexOf(startChars[1]);
				openedBracketsIndex = jsonText.indexOf(startChars[0]);
				if (closedCurlybracesIndex != -1 || closedBracketIndex != -1 || openedCurlybracesIndex != -1
						|| openedBracketsIndex != -1) {
					min = getMinof(openedCurlybracesIndex, closedCurlybracesIndex, openedBracketsIndex,
							closedBracketIndex);
					i += min;
				}
			}
		}
	}

	private ArrayList<String> getStringAsTokens(String jsonFragment) {
		ArrayList<String> result = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();
		char jasonFragmentChar[] = jsonFragment.toCharArray();
		String key = "", val = "";
		int cfgLen = jasonFragmentChar.length, dqCnt = 0;
		for (int j = 0; j < cfgLen; j++) {
			if ((int) jasonFragmentChar[j] == 92) {
				sb.append(jasonFragmentChar[j]).append(jasonFragmentChar[j + 1]);
				j++;
				continue;
			}
			if (jasonFragmentChar[j] == '"') {
				dqCnt++;
			}
			if (dqCnt == 1) {
				sb.append(jasonFragmentChar[j]);
			}
			if (sb.toString().trim().length() == 0) {
				continue;
			}
			if (dqCnt == 2) {
				sb.append(jasonFragmentChar[j]);
				key = sb.toString();
				result.add(key);
				dqCnt = 0;
				sb.delete(0, sb.length());
			}
		}
		isValidTokens(jsonFragment, result);
		return result;
	}

	public boolean isValidTokens(String jsonFragment, ArrayList<String> result) {
		if (jsonFragment.trim().length() <= 1) {
			return false;
		}

		String data[] = jsonFragment.split(","), tokens[] = null;
		int len = data.length;
		for (int i = 0; i < len; i++) {
			if (data[i].contains(":")) {
				tokens = data[i].split(":");
				if (result.contains(tokens[0].trim())) {
					result.remove(tokens[0].trim());
				} else {
					throw new NotAValidToken(tokens[0].trim());
				}
				if (result.contains(tokens[1].trim())) {
					result.remove(tokens[1].trim());
				} else {
					throw new NotAValidToken(tokens[1].trim());
				}
			} else {
				if (result.contains(data[i].trim())) {
					result.remove(data[i].trim());
				} else {
					throw new NotAValidToken(data[i].trim());
				}
			}
		}

		return false;
	}

	public void updateDBwithArray(ArrayList<String> cfg, String key, int startIndex) {
		int len = cfg.size(), aux = 0;
		if (key.trim().length() == 0) {
			return;
		}
		if (len == 1 && cfg.get(0) == "$") {
			return;
		}
		for (int i = 0; i < len; i++) {
			aux = i + startIndex;
			if (key.trim().endsWith(".")) {
				dataBase.put(key + "[" + aux + "]", cfg.get(i));
			} else {
				dataBase.put(key + ".[" + aux + "]", cfg.get(i));
			}
		}
		aux++;
		indexStack.pop();
		indexStack.push(aux);
		cfg.clear();

	}

	private void updateDataBase(String key, String val) {
		dataBase.put(key, val);
	}

	public void updateDBwithMap(ArrayList<String> elements, String key) {
		int len = elements.size();
		for (int i = 0; i + 1 < len; i = i + 2) {
			if (key.trim().endsWith(".")) {
				dataBase.put(key + elements.get(i), elements.get(i + 1));
			} else {
				dataBase.put(key + "." + elements.get(i), elements.get(i + 1));
			}
		}

	}

	private int getMinof(int openedCurlybracesIndex, int closedCurlybracesIndex, int openedBracketsIndex,
			int closedBracketIndex) {
		int min = Integer.MAX_VALUE;
		if (openedCurlybracesIndex >= 0 && (min > openedCurlybracesIndex)) {
			min = openedCurlybracesIndex;
		}
		if (closedCurlybracesIndex >= 0 && (min > closedCurlybracesIndex)) {
			min = closedCurlybracesIndex;
		}
		if (openedBracketsIndex >= 0 && (min > openedBracketsIndex)) {
			min = openedBracketsIndex;
		}
		if (closedBracketIndex >= 0 && (min > closedBracketIndex)) {
			min = closedBracketIndex;
		}
		return min;
	}

	public static void main(String[] args) {
// TODO Auto-generated method stub
		System.out.println("Jesus is Lord:Romans-10:9");
		System.out.println("---------------------------------------");
		AcaciaArkJSONParser jp = new AcaciaArkJSONParser("C:\\slingBuilder\\workspace\\tools\\test.json");
	}

}

class NotAValidToken extends RuntimeException {
	public NotAValidToken() {
		super();
	}

	public NotAValidToken(String msg) {
		super(msg);
	}
}
