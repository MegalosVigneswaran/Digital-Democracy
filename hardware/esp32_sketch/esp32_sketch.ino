/*
Author : Vigneswaran S
*/

/*
Componets(Used in this code):
-ESP32
-20x4 LCD display ---> 5V
-4*3 numpad
-RTC module ---> 5V
-Voltage measuring module
-SD card module ----> 5V
-buzzer (num 1)
-LED (num 2) for power and charging
-Push button
*/

/*
Allocated Pins:

Keypad

    R1 ----> GPIO 12
    R2 ----> GPIO 14
    R3 ----> GPIO 27
    R4 ----> GPIO 26
    C1 ----> GPIO 25
    C2 ----> GPIO 33
    C3 ----> GPIO 32

LCD Display (I2C)

    SDA (LCD) ----> GPIO 21
    SCL (LCD) ----> GPIO 22

RTC Module (I2C)

    SDA (RTC) ----> GPIO 21
    SCL (RTC) ----> GPIO 22

Voltage Measuring Module

    Analog Out or S (Voltage Module) ----> GPIO 35

SD Card Module (SPI)

    MOSI (SD) ----> GPIO 23
    MISO (SD) ----> GPIO 19
    SCK (SD) ----> GPIO 18
    CS (SD) ----> GPIO 5

Buzzer

    Positive (Buzzer) ----> GPIO 17

LEDs for Power and Charging

    LED 1 (Power) ----> GPIO 2
    LED 2 (Charging) ----> GPIO 4

Power and Ground Connections

    VCC (All components) ----> VIN
    GND (All components) ----> GND
*/


/*
Key-formats:
time format : DD-MM-YYY-HH-MM
*/

/*
Files in the SD card:
election.json
secretary.json
chairman.json
result.json
st-xxxxxxx NOTE: This file is used indicated the students is voted or not

*/

#include <SD.h>
#include <SPI.h>
#include <Wire.h>
#include <Keypad.h>
#include <RTClib.h>
#include <Dictionary.h>
#include <ArduinoJson.h>
#include <BluetoothSerial.h>
#include <LiquidCrystal_I2C.h>

int end_time;
int entry_no;
const byte ROWS = 4;
const byte COLS = 3;
const int buzzer = 17;
const int verifb = 4;
const int chip_select = 5;
const int voltage_pin = 35;

bool vbs = false;
bool pass = false;
bool show_result = false;
bool election_now = false;

char keys[ROWS][COLS] = {
  {'1','2','3'},
  {'4','5','6'},
  {'7','8','9'},
  {'*','0','#'}
};

String CkeysArray[30];
String CvaluesArray[30];
String SkeysArray[30];
String SvaluesArray[30];

Dictionary SResultArray;
Dictionary CResultArray;

DateTime time_s;
DateTime time_st;

byte rowPins[ROWS] = {12, 14, 27, 26};
byte colPins[COLS] = {25, 33, 32}; 

RTC_DS3231 RealTC;
BluetoothSerial SerialBT;
LiquidCrystal_I2C display(0x27,20,4);
Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, ROWS, COLS );

void setup() {

  Serial.begin(9600);
  SerialBT.begin("q1w2e3r4");
  Wire.begin();
  RealTC.begin();
  display.init();
  display.backlight();
  pinMode(buzzer,OUTPUT);
  pinMode(verifb, INPUT);
  pinMode(voltage_pin,INPUT);

  String checkfiles[4] = {"election.json","result.json","secretary.json","chairman.json"};

  if (!SD.begin(chip_select)) {
    display.clear();
    display.setCursor(1,0);
    display.print("SD card failure!");
    display.setCursor(2,0);
    display.print("Check & restart");
    for(;;);
  }

  for(String files : checkfiles){
    if(!SD.exists(files)){
      display.clear();
      display.setCursor(0,1);
      display.print("File missing");
      display.setCursor(0,2);
      display.print("Check & restart");
      for(;;);
    }
  }

  election_onini();

  if(election_on()){
    election_now = true;
    get_candidate("secretary.json",SkeysArray,SvaluesArray); //Get secretary info
    get_candidate("chairman.json",CkeysArray,CvaluesArray); //Get chairman info
  };

}

void loop() {

  if(SerialBT.available()){

    SerialBT.println(" ");
    String in = SerialBT.readString();

    if(in=="digitaldemocracy"){
      pass=true;
    }

    else if(!election_now && show_result && in == "showresult"){
      send_result();
    }

    else if(in.indexOf("\"type\":\"add election\"") != -1 && !election_now ){
      AdddElection(in);
    }

  }
  
  if(digitalRead(verifb) == HIGH && !vbs && pass && election_now){
    vbs = true;
    getvote();
  }
  else if(digitalRead(verifb) == LOW){
    vbs = false;
  }

}

void AdddElection(String election_detail){
  /*
  Format of string data:
  {
    "type":"add election",

    "election.json":{
      "times":"election start time",
      "timest":"election end time",
    },

    "secretary.json":{
      "code":"name"
    },

    "chairman.json":{
      "code":"name"
    },

    "result.json":{
      "secretary":{
        "code":"votes"
      },
      "chairman":{
        "code":"votes"
      }
    }
  }
  */

  DynamicJsonDocument doc(300);
  deserializeJson(doc,election_detail);

  String text;
  for (JsonPair kv : doc.as<JsonObject>()) {

    String key = kv.key().c_str();

    if(key != "type"){
      text = "";
      serializeJsonPretty(kv.value(), text);
      File file = SD.open(key,FILE_WRITE);
      file.print(text);
      file.flush();
      file.close();
    }
  }

}
DateTime parseDateTime(String dateTimeStr) {

  int day = dateTimeStr.substring(0, 2).toInt();
  int month = dateTimeStr.substring(3, 5).toInt();
  int year = dateTimeStr.substring(6, 10).toInt();
  int hour = dateTimeStr.substring(11, 13).toInt();
  int minute = dateTimeStr.substring(14, 16).toInt();

  return DateTime(day, month, year, hour, minute);
}
void election_onini(){

  /*
  election.json format:
  {
    "times":"DD-MM-YYYY-HH-MM",
    "timest":"DD-MM-YYYY-HH-MM",
  }*/

  File election_ = SD.open("election.json");
  String electionstring = "";
  while (election_.available()) {
    electionstring += (char)election_.read();
  }
  election_.close();
  StaticJsonDocument<200> edoc;

  deserializeJson(edoc, electionstring);

  String times = edoc["times"].as<String>();
  String timest = edoc["timest"].as<String>();

  time_s = parseDateTime(times);
  time_st = parseDateTime(timest);

}

bool election_on(){

  DateTime dt = RealTC.now();
  dt = DateTime(dt.day(),dt.month(),dt.year(),dt.hour(),dt.minute());

  if(time_s <= dt && dt <= time_st){
    return true;
  }
  else if(dt > time_st){
    show_result = true;
    return false;
  }
  else{
    return false;
  }

}
void get_candidate(String filename , String keyarray[] , String valuearray[]){
  /*
  Format of candidates files(example):
  [
    "99":"yy",
    "10":"xx"
  ]
  */
  int numPairs = 0;
  String jsonString = "";
  File file = SD.open(filename);
  while (file.available()) {
    jsonString += (char)file.read();
  }
  file.close();
  DynamicJsonDocument doc(200);
  DeserializationError error = deserializeJson(doc, jsonString);

  for (JsonPair kv : doc.as<JsonObject>()) {
    keyarray[numPairs] = kv.key().c_str();
    valuearray[numPairs] = kv.value().as<String>();
    numPairs++;
  }
}

void getvote(){

  display.clear();

  display.setCursor(0,2);
  display.print("Enter secretary code");
  select_candidate("secretary",SkeysArray,SvaluesArray);
  clear_row(2);
  display.print("Enter chairman code");
  select_candidate("chairman",CkeysArray,CvaluesArray);

  //greeting
  display.setCursor(0,0);
  display.print("--------------------");
  display.setCursor(0,1);
  display.print("|");
  display.setCursor(3,1);
  display.print("Thank you!");
  display.setCursor(19,1);
  display.print("|");
  display.setCursor(0,2);
  display.print("|");
  display.setCursor(19,2);
  display.print("|");
  display.setCursor(0,3);
  display.print("--------------------");

}
void select_candidate(String c_position , String keyarray[] , String valuearray[] ){

  int numPairs = 0;
  int keyCount = 0;
  String candy = "";

  display.setCursor(8,4); 

  while (keyCount < 2) {
    char key = keypad.getKey();
    if (key) { 
      for (int j = 0; j < numPairs; j++) {
        String ke = keyarray[j];
        if (ke.indexOf(candy + key) > -1) { 
          candy += key; 
          keyCount++; 
          digitalWrite(buzzer,HIGH);
          delay(100);
          digitalWrite(buzzer,LOW);
          break;
        }
      }
    }
  }

  //Update the result.json
  String jsonString = "";
  File file = SD.open("result.json");
  while(file.available()){
    jsonString += (char)file.read();
  }
  DynamicJsonDocument doc(250);
  deserializeJson(doc, jsonString);
  doc[c_position][candy] = doc[c_position][candy].as<int>() + 1;  serializeJson(doc, jsonString);
  serializeJson(doc,jsonString);
  file.close();
  file = SD.open("result.json",FILE_WRITE);
  file.print(jsonString);
  file.close();

  digitalWrite(buzzer,HIGH);
  for (int i = 0; i < numPairs; i++) {
    if (keyarray[i] == candy) {
      display.clear();
      display.setCursor(1,1);
      display.print("you are selected");
      display.setCursor(0,2);
      display.print(valuearray[i]);
      break;
    }
  }
  delay(1000);
  digitalWrite(buzzer,LOW);
}

void clear_row(int row){
  
  for(int i = 0  ; i <20 ; i++){
    display.setCursor(i,row);
    display.print(" ");
  }
}

void readFileToString(const char* filename, String& fileContent) {
  File file = SD.open(filename);
  if (!file) {
    Serial.print("Failed to open ");
    Serial.println(filename);
    return;
  }

  fileContent = "";
  while (file.available()) {
    fileContent += (char)file.read();
  }
  file.close();
}

void send_result(){


  /*
  json data format (result.json):
  {
    "secretary":{
      "code <String>":"votes <int>,
      "code <String>":"votes <int>"
      ....
    },
    "chairman":{
      "code <String>":"votes <int>",
      "code <string>":"votes <int>"
      ....
    }
  }
  
  Json data format (secretary.json)
  {
    "code<String>":"name<String>"
    ...
  }

  Json data format (chairman.json)
  {
    "code<String>":"name<String>"
    ...
  }

  Final text json format:
  {
    "secretary":{
      "code <String>":"votes <int>,
      "code <String>":"votes <int>"
      ....
    },
    "chairman":{
      "code <String>":"votes <int>",
      "code <string>":"votes <int>"
      ....
    }

    "secretary.json":{
      "code<String>":"name<String>"
      ...
    }
    
    "chairman.json":{
      "code<String>":"name<String>"
      ...
    }
  }
  */

  String resultJson, secretaryJson, chairmanJson;

  readFileToString("result.json", resultJson);
  readFileToString("secretary.json", secretaryJson);
  readFileToString("chairman.json", chairmanJson);

  DynamicJsonDocument doc(1024); 

  DynamicJsonDocument resultDoc(512);
  DynamicJsonDocument secretaryDoc(256);
  DynamicJsonDocument chairmanDoc(256);

  DeserializationError error = deserializeJson(resultDoc, resultJson);
  if (error) {
    Serial.print("Failed to parse result JSON: ");
    Serial.println(error.c_str());
  }

  error = deserializeJson(secretaryDoc, secretaryJson);
  if (error) {
    Serial.print("Failed to parse secretary JSON: ");
    Serial.println(error.c_str());
  }

  error = deserializeJson(chairmanDoc, chairmanJson);
  if (error) {
    Serial.print("Failed to parse chairman JSON: ");
    Serial.println(error.c_str());
  }

  doc["secretary"] = resultDoc["secretary"];
  doc["chairman"] = resultDoc["chairman"];
  doc["secretary_json"] = secretaryDoc;
  doc["chairman_json"] = chairmanDoc;

  String jsonString;
  serializeJson(doc, jsonString);

  SerialBT.println(jsonString);
  
}

