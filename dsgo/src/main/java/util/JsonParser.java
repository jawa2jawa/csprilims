

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

import org.json.JSONObject;

public class JsonParser {
	File jsonFile = null;
	Stack<String> keyStack = new Stack<String>();
	Stack<Integer> indexStack = new Stack<Integer>();
	Stack<Character> operatorStack = new Stack<Character>();
	JsonDB dataBase = new JsonDB();
	ArrayList<String> elements = new ArrayList<String>();
	char startChars[] = { '[', '{' }, previousClosed = ' ';
	HashMap<Character, Character> endChMap = new HashMap<Character, Character>();
	String previousData = "", nextKey = "";
	Integer nextIndex = 0;

	public ArrayList<String> getJsonFragemts() {
		return jsonFragemts;
	}

	public void setJsonFragemts(ArrayList<String> jsonFragemts) {
		this.jsonFragemts = jsonFragemts;
	}

	ArrayList<String> jsonFragemts = new ArrayList<String>();

	public JsonParser(String fileNmae) {
		jsonFile = new File(fileNmae);
		endChMap.put('{', '}');
		endChMap.put('[', ']');
		keyStack.push("$");
		parse();
		System.out.println("**********************DB*********************");
		printDataBase();
		System.out.println("**********************KEYS*********************");
		printAccountDetails();
	}

	private String asJsonString(String key) {
		String result = key;
		if (!key.startsWith("\"")) {
			result = "\"" + result;
		}
		if (!key.endsWith("\"")) {
			result = result + "\"";
		}
		return result;
	}

	private void printAccountDetails() {
		Set set = dataBase.keySet();
		String prefix = ".development.roku-roku.beta.", data[] = null, aux = "", key = "", val = "", jsonResult = "";
		ArrayList keys = new ArrayList<String>(set);
		int len = keys.size();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			if (!((String) keys.get(i)).contains("beta")) {
				continue;
			}
			data = ((String) keys.get(i)).split("beta");
			aux = data[1];
			data = aux.split("]");
			data[0] = data[0] + "]";
			sb.append("{");
			System.out.println(keys.get(i));
			for (int j = i; j < len; j++) {
				aux = (String) keys.get(j);
				if (aux.contains(data[0])) {
					data = aux.split("].");
					sb.append(asJsonString(data[1])).append(":").append(asJsonString(dataBase.get(aux))).append(",");
				} else {
					i = j;
					jsonResult = sb.toString();
					sb.delete(0, sb.capacity());
					jsonResult = jsonResult.substring(0, jsonResult.length() - 1);
					break;
				}
			}
			sb.append(jsonResult).append("}");
			jsonFragemts.add(sb.toString());
			System.out.println("Json Fragment:\n" + sb.toString());
			String auxStr= "{\"account\":" +sb.toString()+ "}";
			JSONObject jsonObj=new JSONObject(auxStr);
			jsonObj=jsonObj.getJSONObject("account");
			System.out.println(jsonObj.toString());
			sb.delete(0, sb.capacity());
		}
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
				parseData(sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	StringBuilder jsonFragment = new StringBuilder();
	boolean isArray = false;

	char currentCh = ' ';

	private void parseData(String jsonData) {
		int len = jsonData.length();
		for (int i = 0; i < len; i++) {
			currentCh = jsonData.charAt(i);
			if (currentCh != '{' && currentCh != '}' && currentCh != '[' && currentCh != ']') {
				jsonFragment.append(currentCh);
			}
			if (currentCh == '{' || currentCh == '[' || currentCh == '}' || currentCh == ']') {
				if (currentCh == '{' || currentCh == '[') {
					operatorStack.push(currentCh);
				}
				updateDB();
				jsonFragment.delete(0, jsonFragment.length());
			}
		}

	}

	private String getNextKey(String key) {
		int len = key.length();
		int sindex = 0, eindex = 0, index = 0;
		String result = "";
		for (int i = len - 1; i >= 0; i--) {
			if (key.charAt(i) == '[') {
				sindex = i + 1;
				for (int j = i; j < len; j++) {
					if (key.charAt(j) == ']') {
						eindex = j;
						index = Integer.parseInt(key.substring(sindex, eindex));
						result = key.substring(0, i);
						index++;
						return result + "[" + index + "]";
					}
				}
			}
		}

		return key;
	}

	private void updateDB() {
		if (keyStack.isEmpty()) {
			return;
		}
		String jsonStr = jsonFragment.toString().trim();
		String currentKey = "", key = "";
		if (jsonStr.length() == 0 && keyStack.peek().equals("$")) {
			return;
		}

		if (jsonStr.length() == 0 && (currentCh == '{' || currentCh == '[')) {
			key = keyStack.peek();
			key = key + " ";
			keyStack.push(key);
			return;
		}

		if (jsonStr.length() == 0 && (currentCh == '}' || currentCh == ']')) {
			key = keyStack.pop();
			if (key.contains("prod")) {
				System.out.println("Popped Key===>" + key);
			}
			if (key.contains("beta")) {
				System.out.println("Popped Key===>" + key);
			}
			return;
		}

		if (jsonStr.length() == 1 && jsonStr.charAt(0) == ',') {
			key = keyStack.pop();
			key = getNextKey(key);
			keyStack.push(key);
			keyStack.push(key + " ");
			return;

		}
		if (jsonStr.trim().startsWith(",")) {
			if (operatorStack.peek() == '[') {
				key = keyStack.pop();
				keyStack.push(getNextKey(key));
			}
		}
		String aux[] = jsonStr.split(","), tokens[] = null;
		int len = aux.length, index = 0;
		if (keyStack.isEmpty()) {
			return;
		}
		key = keyStack.peek();

		if (jsonStr.endsWith(",")) {
			// continue,it has key
			for (int i = 0; i < len; i++) {
				dataBase.put(key, aux[i].trim());
				key = getNextKey(key);

			}
			keyStack.pop();
			keyStack.push(key);
			keyStack.push(key + " ");
			return;
		}

		if (jsonStr.endsWith(":")) {
			// continue,it has key
			if (jsonStr.contains("prod")) {
				System.out.println(jsonStr);
			}
			for (int i = 0; i < len - 1; i++) {
				if (aux[i].trim().length() == 0) {
					continue;
				}
				tokens = getKVPair(aux[i]);
				if (tokens.length == 2) {
					if (key.trim().endsWith(".")) {
						dataBase.put(key.trim() + tokens[0].trim(), tokens[1].trim());
					} else {
						dataBase.put(key.trim() + "." + tokens[0].trim(), tokens[1].trim());
					}
				} else {
					dataBase.put(key, tokens[0].trim());
					key = getNextKey(key);
					index++;
				}
			}
			currentKey = (aux[len - 1].substring(0, aux[len - 1].length() - 1)).trim();
			if (operatorStack.peek() == '[') {
				keyStack.push(key.trim() + "." + currentKey + "[0]");
			} else {
				keyStack.push(key.trim() + "." + currentKey);
			}
			return;
		}

		// complete
		for (int i = 0; i < len; i++) {
			if (aux[i].trim().length() == 0) {
				continue;
			}
			tokens = getKVPair(aux[i]);
			if (tokens.length == 2) {
				if (key.trim().endsWith(".")) {
					dataBase.put(key.trim() + tokens[0].trim(), tokens[1].trim());
				} else {
					dataBase.put(key.trim() + "." + tokens[0].trim(), tokens[1].trim());
				}
			} else {
				if (tokens[0].trim().length() > 0) {
					dataBase.put(key.trim(), tokens[0].trim());
					key = getNextKey(key);
				}
			}
		}
		key = keyStack.pop();
		operatorStack.pop();
	}

	private String[] getKVPair(String str) {
		String data1[] = new String[1];
		int len = str.length(), index = 0;
		char prevCh = ' ', currentCh = ' ';
		for (int i = 0; i < len; i++) {
			currentCh = str.charAt(i);
			if (currentCh == ' ') {
				continue;
			}
			if (currentCh == ':') {
				index = i;
			}
			if (currentCh == '"' && prevCh == ':') {
				String data[] = new String[2];
				data[0] = str.substring(0, index);
				data[1] = str.substring(index + 1, len);
				return data;
			}
			prevCh = currentCh;
		}
		data1[0] = str;
		return data1;

	}

	public static void main(String[] args) {
// TODO Auto-generated method stub
		System.out.println("Jesus is Lord:Romans-10:9");
		System.out.println("---------------------------------------");
		JsonParser jp = new JsonParser("C:\\codeSpace\\buildMaster\\src\\test\\resources\\Accounts_roku-roku.json");
	}

	class JsonDB extends LinkedHashMap<String, String> {
		private String trimQuates(String str) {
			StringBuilder sb = new StringBuilder();
			int len = str.length();
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) == '"' || str.charAt(i) == '$') {
					continue;
				}
				sb.append(str.charAt(i));
			}

			return sb.toString();
		}

		public String put(String key, String val) {
			key = trimQuates(key);
			val = trimQuates(val);

			return super.put(key, val);
		}
	}
}
