package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;


public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_TERM = ""; // no restriction
	private static final String API_KEY = "NtDgAsh381ComX4x3nQdQsPAsC5cC37p";
	
    public List<Item> search(double lat, double lon, String term) {
    	if(term == null) {
    		term = DEFAULT_TERM;
    	}
    	try {
    		term = java.net.URLEncoder.encode(term, "UTF-8");
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
    	String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, term, 50);
    	
    	try {
    		HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
    		connection.setRequestMethod("GET");
    		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    		StringBuilder response = new StringBuilder();
    		String line = "";
    		while((line = br.readLine()) != null) {
    			response.append(line);
    		}
    		br.close();
    		JSONObject jsob = new JSONObject(response.toString());
    		if(jsob.isNull("_embedded")) {
    			return new ArrayList<Item>();
    		}else{
    			JSONObject embedded = jsob.getJSONObject("_embedded");
    			JSONArray events = embedded.getJSONArray("events");    	
    			return getItemList(events);
    		}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return new ArrayList<Item>();
    }
	
	private JSONObject getVenue(JSONObject event) throws JSONException {
		if(!event.isNull("_embedded")) {
			JSONObject jsob = event.getJSONObject("_embedded");
			if(!jsob.isNull("venues")) {
				JSONArray venues = jsob.getJSONArray("venues");
				if(venues.length()>= 0) {
					return venues.getJSONObject(0);
				}
			}
		}
		return null;
	}

	private String getImageUrl(JSONObject event) throws JSONException {
		if(!event.isNull("images")) {
			JSONArray array = event.getJSONArray("images");
			for(int i = 0; i < array.length(); i++) {
				JSONObject image = array.getJSONObject(i);
				if(!image.isNull("url")) {
					return image.getString("url");
				}
			}
		}
		return null;
	}

	private Set<String> getCategories(JSONObject event) throws JSONException {
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			Set<String> categories = new HashSet<>();
			for (int i = 0; i < classifications.length(); i++) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						String name = segment.getString("name");
						categories.add(name);
					}
				}
			}
			return categories;
		}
		return null;
	}

	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		for(int i = 0; i< events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			
			Item.ItemBuilder builder = new ItemBuilder();
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}

			JSONObject venue = getVenue(event);
			if(venue != null) {
				StringBuilder sb = new StringBuilder();
				if(!venue.isNull("address")) {
					JSONObject address = venue.getJSONObject("address");
					if (!address.isNull("line1")) {
						sb.append(address.getString("line1"));
					}
					if (!address.isNull("line2")) {
						sb.append(address.getString("line2"));
					}
					if (!address.isNull("line3")) {
						sb.append(address.getString("line3"));
					}					
					sb.append(",");
				}
				if (!venue.isNull("city")) {
					JSONObject city = venue.getJSONObject("city");
					if (!city.isNull("name")) {
						sb.append(city.getString("name"));
					}
				}
				builder.setAddress(sb.toString());				
			}
			builder.setImageUrl(getImageUrl(event));
			builder.setCategories(getCategories(event));
			
			Item item = builder.build();
			itemList.add(item);				
			
		}
		return itemList;
		
	}
	
}
