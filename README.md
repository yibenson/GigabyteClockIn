# Clockin App

## Description
Clockin is a general purpose time-tracking app that allows users to punch-in and out securely via facial recognition. Users can keep track of their own user profiles and punch records; managers have the ability to add new users, edit their information, and view and edit company-wide punch records.

Demo: https://drive.google.com/file/d/1UVupcJa-Zw9dS2Vcw7_NkH1vDeZbt-lL/view?usp=sharing

## Preliminaries

### SSL

To make a server request, the app must first establish an SSL connection with the server, and therefore complete the SSL handshake. The classes responsible for aiding the handshake are located under the 'Volley' folder. Essentially for the app to trust a server and establish an encrypted connection, servers are configured with a certificate that contains a public and private key (used for cryptographic handshakes). Most servers are configured with certificates from well-known issuers, that act like trustworthy middlemen. However, our server has a self-signed certificate that I imported directly into the application. I think if we scale the app and plan to publish it online, we should probably use a trusthworthy certificate because in order to use our self-signed certificate, I had to create a lot of code that I don't understand very well (again, located under Volley). Official documentation can be located here: https://developer.android.com/training/articles/security-ssl

### Volley

To make a request, all information must be inserted into a HashMap and inserted into a VolleyDataRequester object, as seen in the example below. The response from the server can be located in the "response" object, which is a JSONObject. More detailed formats about response (which is different for different requests) can be read on the CloudMatrix API page.

The URL field must hold the IP address of the request we are making. 

Don't forget to add .requestJSON() at the end

```aidl
  void loginButtonClicked() throws MalformedURLException {
        ...
            HashMap<String, String> body = new HashMap<>();
            body.put("password", password);
            body.put("account", email);
            VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                    .setUrl( host + "login/login")
                    .setBody( body )
                    .setMethod( VolleyDataRequester.Method.POST )
                    .setJsonResponseListener(response -> {
                        try {
                            if (response.getBoolean("status")) {
                                Intent intent = new Intent(getApplicationContext(), FaceClockIn.class);
                                intent.putExtra("PURPOSE", "IDENTIFY");
                                intent.putExtra("ACCOUNT", binding.email.getText().toString());
                                startActivity(intent);
                            } else {
                                // user/pw invalid
                                showAlertDialog("login_error");
                            }
                        } catch (JSONException e) {
                            // error connecting to server
                            showAlertDialog("error");
                        }
                    })
                    .requestJson();
        }
    }


```

### MainActivity
Landing page of the app, with email/password fields and a "Register" button for the company. Lester and I spent a lot of time trying to make Google sign-in work, but it's very difficult to figure out. When the user clicks the login button, we call the function above and send a request to the server. If "status" is true, then the login succeeded and we go to the FaceClockIn activity.

### FaceClockIn
There are 3 use cases for when we want to use the camera:
1) To identify a user for login.
2) To take a photo while we are creating a new user
3) To edit the photo of an existing user and upload to server

In the bindUseCases() function (which is not related to the use cases above), we see our basic implementation of CameraX functionality. The Preview section sets up the live streaming for the camera; the ImageAnalysis detects if there is a face and if the eyes are blinking; and the ImageCapture takes a photo if the ImageAnalysis analzer says the face is alive (eyes are blinking). Then ImageCapture does a lot of cropping around the bounding box of the face and asks the user to confirm the photo before sending.

Where we send the photo depends on our 3 use cases. If we are identifying a user for login, then we send the photo and get back all the information we need to unlock the rest of the account. If we are just registering, then we send the photo back to the UserRegistrationWindow class, where the user can decide to continue creating their profile and upload to the server from there. If we are editing, then we send it to the server directly.

For the liveness analyzer, we basically store the probability that the left eye is open for the 30 most recent frames. If the probability varies over time, then we say the face is live. There is a threshold variable that you can adjust if you want tighter control over the liveness feature. So far, Google ML SDK has no liveness detection feature of its own, so we have to do stupid things like this.

More documentation on CameraX can be found here:https://developer.android.com/training/camerax

### Homepage
If the user logs in correctly, they will see the Homepage. There are buttons on the top and bottom to clock in/out; if the user presses one of them, they are prompted with a confirm page and we will send the request to the server. There is also a button in the top right for managers to sign in with a secure pin; the reason the code looks kind of strange for this section is because I try to make the cursor bounce automatically to each square after you input 1 number of the pin. It will remain for a future coder on how to hide the numbers. 

Currently, all times are handled via Calendar objects and string parsing. Calendar is an old library though and is hard to use/hard to debug; I've been using LocalDateTime and LocalDate to handle punches for management and that's been much easier. You might want to transfer Calendar over to these two objects to make your life easier.

## Users

### Punches
The punch records page contains a list of the user records for the requested date range. Users can pull down to refresh the feed and also get the next set of records for the 30 days. There is a bug right now with records not showing up - I think it has something to do with the Calendar/LocalDateTime not being compatible, and will try to fix asap. 

Essentially, we maintain a list of punch records and everytime we refresh the layout, we add to the top of the list and push it into the screen. Pretty simple. 

### User profile 
When we first start the page, we send a request to the server for the user information to fill the page. Clicking on any of the fields will bring the user to an EditPage where they can edit their information. They can also change their profile picture.

## Managers

### Management Menu
If the user enters their pin number, they can access the management menu. Since many of the buttons in the Figma are associated with editing, maybe we have some duplicate or unneeded buttons - anyways, code here is also very simple. 

### Management Punches
This was the most complicated portion of the application. Because we need a list of dates, with each date containing a list of punch entries for that date, we have to create something called a SectionedRecyclerView. RecyclerViews are commonly used in Android to store lists of things: a list of names, or messages, or something. But they all usually have the same layout, like maybe each element is one row of text. Here, we sometimes need to insert a row of text (for the date). Sometimes we need to insert a punch entry. So I used the SectionedRecyclerView library to do this. To understand this section, you should read more about RecyclerViews and how they are implemented (pay a lot of attention to ViewHolders and what they do inside RecyclerView). 

The important information is contained in "Child" object. Child contains a particular punch entry: the in/out time and date, the name of the puncher, the total time they worked. A SectionHeader is basically our Date section - it has a date and a list of Child objects (the punch entries for that date). When we start the ManagementPunches page, we first request the faces of all the employees in the company from the server and store them in a JSONObject (the getFaces() function). We then try to get a list of SectionHeaders in populateDict - this list will contain SectionHeaders for each date in the requested time range, and each SectionHeader will contain a list of Child punch entries for that date. 

There's some difficulty in parsing the response from the server. To get punch records from the server, you make a request to cal_working_hours, which gives you a response object that looks like:
```aidl
{..., "result": {"Name of employee 1: {..., "detail": JSONArray of punch records
```
Each punch record in that JSONArray is also a JSONArray. From the individual punch record JSONArray, we need to get the date and put it into our SectionHeader list if it's not there yet. Then we put the individual punch record into the correct SectionHeader in that list. 

We also maintain a HashMap of dates; this lets us efficiently look up if we have already put a date into our list of SectionHeaders. Furthermore, the dates HashMap will also store the index of the SectionHeaders; this is important because the RecyclerView will display each section according to its "index" attribute. So later, we can sort our list of dates and set the index attributes of the corresponding SectionHeaders so the sections appear in order.

```aidl
 for (Iterator<String> it = result.keys(); it.hasNext();) {
    String name = it.next();
    JSONObject jsonObject = result.getJSONObject(name);
    JSONArray detail = jsonObject.getJSONArray("detail"); // this json array contains json arrays of each clockin/out entry
    for (int i = 0; i < detail.length(); i++) {
        JSONArray punch_times = detail.getJSONArray(i); // contains one clockin/out entry for one user
        LocalDateTime date = LocalDateTime.parse(punch_times.getString(0), BASE_FORMAT); // gets clockin time for this entry
        String date_string = date.format(DAY_FORMAT); // gets date/year of string
        if (!dates.has(date_string)) {
            dates.put(date_string, index);
            sectionHeaderList.add(new SectionHeader(new ArrayList<>(), date_string, 0));
               index++;
            }
            punch_times.put(name); // adds name of entry to the json array (VERY IMPORTANT)
                                     // puts the entry into the punches table
            int sectionIndex = dates.getInt(date_string);
            sectionHeaderList.get(sectionIndex).childList.add(new Child(punch_times));
   }
}
```

### Edit Punches (part of Management Punches)
This section will not make sense to you unless you understand RecyclerView / ViewHolder. But basically, we create a click interface inside the ChildViewHolder and implement it inside ManagementPunches. We can then pass the implementation to each ChildViewHolder when we bind it inside our RecyclerView adapter so that when we click on a button, we know which row/entry we are clicking on. 

The rest is pretty simple - a page will pop up that shows the punch record and you can choose to edit it. 

### UserRegistrationWindow



















