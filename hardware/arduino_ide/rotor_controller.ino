#include <hardware/pwm.h>

const int port1 = 2;
const int port2 = 4;
const int port3 = 6;
const int port4 = 8;

uint port1_slice, port2_slice, port3_slice, port4_slice;

void setup() {
  Serial.begin(115200);
  gpio_set_function(port1, GPIO_FUNC_PWM);
  gpio_set_function(port2, GPIO_FUNC_PWM);
  gpio_set_function(port3, GPIO_FUNC_PWM);
  gpio_set_function(port4, GPIO_FUNC_PWM);

  port1_slice = pwm_gpio_to_slice_num(port1);
  port2_slice = pwm_gpio_to_slice_num(port2);
  port3_slice = pwm_gpio_to_slice_num(port3);
  port4_slice = pwm_gpio_to_slice_num(port4);

  
  pwm_set_clkdiv(port1_slice, 244.14);
  pwm_set_clkdiv(port2_slice, 244.14);
  pwm_set_clkdiv(port3_slice, 244.14);
  pwm_set_clkdiv(port4_slice, 244.14);
  pwm_set_wrap(port1_slice, 255);
  pwm_set_wrap(port2_slice, 255);
  pwm_set_wrap(port3_slice, 255);
  pwm_set_wrap(port4_slice, 255);

  pwm_set_chan_level(port1_slice,PWM_CHAN_A, 0);
  pwm_set_chan_level(port2_slice,PWM_CHAN_A, 0);
  pwm_set_chan_level(port3_slice,PWM_CHAN_A, 0);
  pwm_set_chan_level(port4_slice,PWM_CHAN_A, 0);
  
  pwm_set_enabled(port1_slice, true);
  pwm_set_enabled(port2_slice, true);
  pwm_set_enabled(port3_slice, true);
  pwm_set_enabled(port4_slice, true);

}

void validate() {
  Serial.write("*!");
}

void set() {
  int channel = Serial.read();
  int strength = Serial.read();
  char buf[8];
  sprintf(buf, "%02X%02X",channel, strength);
  Serial.print(buf);

  if(channel == -1 || strength == -1)
    return;

  switch(channel) {
    case 0:
      pwm_set_chan_level(port1_slice,PWM_CHAN_A, strength);
      break;
    case 1:
      pwm_set_chan_level(port2_slice,PWM_CHAN_A, strength);
      break;
    case 2:
      pwm_set_chan_level(port3_slice,PWM_CHAN_A, strength);
      break;
    case 3:
      pwm_set_chan_level(port4_slice,PWM_CHAN_A, strength);
      break;
      
  }
}

void keepAlive() {
  Serial.print("kkkk");
}

void loop() {
  int rcv = Serial.read();
  if(rcv != '@')
    return;
  rcv = Serial.read();
  switch(rcv){
    case '?':
      validate();
      break;
    case 's':
      set();
      break;
    case 'k':
      keepAlive();
      break;
  }
}
