#include <Servo.h>

const int TRIG_PIN = 7 ;
const int ECHO_PIN = 2;
const int BUTTON_PIN = 4;
const int SERVO_PIN = 9;
const int LED_PINS[4] = {6, 3, 5, 11};

enum State {
  Read_Magic,
  Read_Key,
  Read_OnOff,
  Read_Mode
};

const byte MAGIC_NUMBER = 0x21;
const byte INFO_KEY  = 0x30;
const byte ERROR_KEY = 0x31;
const byte TIME_KEY  = 0x32;
const byte POT_KEY   = 0x33;
const byte ULTRA_KEY = 0x34;

const byte ONOFF_KEY = 0x40; 
const byte MODE_KEY  = 0x41;

const uint16_t DISPLAY_OFF = 0x3030;
const uint16_t DISPLAY_ON  = 0x3031;

const uint16_t MODE_DIAL     = 0x3d44; //D
const uint16_t MODE_LED_FULL = 0x3d4c; //L
const uint16_t MODE_LED_HALF = 0x3d48; //H
const uint16_t MODE_BOTH     = 0x3d42; //B

const unsigned long SENSOR_INTERVAL = 2000;
const unsigned long DEBOUNCE_DELAY = 10;

enum DisplayMode { OFF, DIAL, LED_FULL, LED_HALF, BOTH };
DisplayMode currentMode = BOTH;
bool displayOn = true;
State nextstate = 0;
int cycle = false;

Servo servo;
unsigned long lastSensor = 0, lastDebounce = 0;
bool btnState = HIGH, lastBtn = HIGH;
int disp = 0;
int onoff = 0;
int index = 0;


long readUltrasonic();
void updateDisplay(int d);
void setLEDs(int p, bool half);
State handleSerial(State state);
void sendUltrasonic(long t);
void sendError(const char *msg);
void sendInfo(const char *msg);
void sendPot(unsigned int pot);
void sendUShort(unsigned int value);
void sendMagicNumber();

void setup() {
  Serial.begin(9600);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  pinMode(LED_PINS[0], OUTPUT);
  pinMode(LED_PINS[1], OUTPUT);
  pinMode(LED_PINS[2], OUTPUT);
  pinMode(LED_PINS[3], OUTPUT);
  servo.attach(SERVO_PIN);
  servo.write(10);
}

void loop() {
  unsigned long now = millis();
  // button debounce
  bool reading = digitalRead(BUTTON_PIN);
  if (reading != lastBtn) lastDebounce = now;
  if (now - lastDebounce > DEBOUNCE_DELAY) {
    if (reading != btnState) {
      btnState = reading;
      if (btnState == LOW) {
        if(currentMode < 4){
          currentMode = currentMode + 1;
        }else{
          currentMode = 1;
        }
      }
    }
  }
  lastBtn = reading;
  // Serial Input
  if((Serial.available() > 0)){
    nextstate = handleSerial(nextstate);
  }

  if (now - lastSensor >= SENSOR_INTERVAL) {
    lastSensor = now;
    long t = readUltrasonic();
    float d = (float) (t * .0343 * .5); 
    if (d < 5) d = 5;
    if (d > 25) d = 25;
    //float d = 100;
    sendUltrasonic(t);
    if (displayOn) updateDisplay(d);
    else setLEDs(0, false);

        unsigned int potValue = analogRead(A0);

    sendPot(potValue);


    if (potValue > 800) {
      sendError("High alarm");
    }else{
      sendInfo("Hello");
    }
  }



}

void sendPot(unsigned int pot) {
  sendMagicNumber();
  Serial.write(POT_KEY);
  sendUShort(pot);
}

void sendMagicNumber() {
  Serial.write(MAGIC_NUMBER);
}

void sendUShort(unsigned int value) {
  Serial.write(value >> 8);
  Serial.write(value);
}

long readUltrasonic() {
  digitalWrite(TRIG_PIN, LOW); delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  return pulseIn(ECHO_PIN, HIGH, 30000);
}

void updateDisplay(float d) {
  float p = (d/25.0) * 100; //  operate over a range of 5 cm to 25 cm (0% to 100% scale)
  if (d < 5) p = 0;
  if (d > 25) p = 100;
  if (currentMode == DIAL) { servo.write(map(p, 0, 100, 10, 170)); setLEDs(0, false); }
  else if (currentMode == LED_FULL)  { servo.write(map(0, 0, 100, 10, 170)); setLEDs(p, false); }
  else if (currentMode == LED_HALF) setLEDs(p, true);
  else if (currentMode == BOTH) { servo.write(map(p, 0, 100, 10, 170)); setLEDs(p, false); }
}

void setLEDs(int p, bool half) {
  if(half){
    if(p >= 80){
      //Serial.println("half >= 80");
      analogWrite(LED_PINS[0], 100);
      analogWrite(LED_PINS[1], 100);
      analogWrite(LED_PINS[2], 100);
      analogWrite(LED_PINS[3], 100);
    }else if(p >= 60){
      analogWrite(LED_PINS[0], 100);
      analogWrite(LED_PINS[1], 100);
      analogWrite(LED_PINS[2], 100);
      analogWrite(LED_PINS[3], 0);
    }else if(p >= 40){
      analogWrite(LED_PINS[0], 100);
      analogWrite(LED_PINS[1], 100);
      analogWrite(LED_PINS[2], 0);
      analogWrite(LED_PINS[3], 0);
    }else if(p >= 20) {
      analogWrite(LED_PINS[0], 100);
      analogWrite(LED_PINS[1], 0);
      analogWrite(LED_PINS[2], 0);
      analogWrite(LED_PINS[3], 0);
    }else{
      analogWrite(LED_PINS[0], 0);
      analogWrite(LED_PINS[1], 0);
      analogWrite(LED_PINS[2], 0);
      analogWrite(LED_PINS[3], 0);
    }
  }else{
     if(p >= 80){
      analogWrite(LED_PINS[0], 255);
      analogWrite(LED_PINS[1], 255);
      analogWrite(LED_PINS[2], 255);
      analogWrite(LED_PINS[3], 255);
    }else if(p >= 60){
      analogWrite(LED_PINS[0], 255);
      analogWrite(LED_PINS[1], 255);
      analogWrite(LED_PINS[2], 255);
      analogWrite(LED_PINS[3], 0);
    }else if(p >= 40){
      analogWrite(LED_PINS[0], 255);
      analogWrite(LED_PINS[1], 255);
      analogWrite(LED_PINS[2], 0);
      analogWrite(LED_PINS[3], 0);
    }else if(p >= 20) {
      Serial.println(p);
      analogWrite(LED_PINS[0], 255);
      analogWrite(LED_PINS[1], 0);
      analogWrite(LED_PINS[2], 0);
      analogWrite(LED_PINS[3], 0);
    }else{
      analogWrite(LED_PINS[0], 0);
      analogWrite(LED_PINS[1], 0);
      analogWrite(LED_PINS[2], 0);
      analogWrite(LED_PINS[3], 0);
    }
  }
}

State handleSerial(State state){
  byte b = Serial.read();
  switch(state){
    case Read_Magic:
      if(b == MAGIC_NUMBER){
        onoff = 0;
        index = 0;
        disp = 0;
        return Read_Key;
      }
    break;
    case Read_Key:
      switch(b){
        case ONOFF_KEY:
          onoff = 0;
          index = 0;
          disp = 0;
          return Read_OnOff;
        break;
        case MODE_KEY:
          onoff = 0;
          index = 0;
          disp = 0;
          return Read_Mode;
        break;
      }
    break;
    case Read_OnOff:
    if(index == 0){
        onoff = b;
      }else{
        onoff = (onoff << 8) | (b);
      }      
      //Serial.println(onoff);
      if(index == 1){
        if(onoff == DISPLAY_ON){ 
          displayOn = 1;
        }
        else if(onoff == DISPLAY_OFF){
          displayOn = 0;
        }
        return Read_Magic;
      }
      index++;
    break;
    case Read_Mode:
      if(index == 0){
        disp = b;
      }else{
        disp = (disp << 8) | (b);
      }
      if(index == 1){
        if (disp == MODE_DIAL){ 
          currentMode = DIAL;
        }
        else if (disp == MODE_LED_FULL){ currentMode = LED_FULL;}
        else if (disp == MODE_LED_HALF) {currentMode = LED_HALF;}
        else if (disp == MODE_BOTH) {currentMode = BOTH;}
        return Read_Magic;
      }
      index++;
    break;
  }
  return state;
}

void sendUltrasonic(long t) {
  Serial.write(0x21);
  Serial.write(ULTRA_KEY);
  Serial.write((t >> 24) & 0xFF);
  Serial.write((t >> 16) & 0xFF);
  Serial.write((t >> 8) & 0xFF);
  Serial.write(t & 0xFF);
}

void sendError(const char *msg) {
  Serial.write(MAGIC_NUMBER);     
  Serial.write(ERROR_KEY);        
  int len = 11;          
  Serial.write((byte)(len >> 8));  
  Serial.write((byte)len); 
  //sending message        
  for(int i = 0; i < len; i++){
  Serial.write(msg[i]);
  }
  Serial.write('\0');

}

void sendInfo(const char* msg) {
  Serial.write(MAGIC_NUMBER);     
  Serial.write(INFO_KEY);        
  int len = 6;          
  Serial.write((byte)(len >> 8));  
  Serial.write((byte)len); 
  //sending message        
  for(int i = 0; i < len; i++){
    Serial.write(msg[i]);
  }
  Serial.write('\0');
}


