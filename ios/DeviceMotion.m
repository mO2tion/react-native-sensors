// Inspired by https://github.com/pwmckenna/react-native-motion-manager

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import "DeviceMotion.h"

@implementation DeviceMotion

@synthesize bridge = _bridge;
RCT_EXPORT_MODULE();

double gravitationalAcceleration = 9.82;

- (id) init {
    self = [super init];
    NSLog(@"DeviceMotion");
    if (self) {
        self->_motionManager = [[CMMotionManager alloc] init];
    }
    return self;
}

RCT_EXPORT_METHOD(setUpdateInterval:(double) interval) {
    NSLog(@"setDeviceMotionUpdateInterval: %f", interval);
    double intervalInSeconds = interval / 1000;

    [self->_motionManager setDeviceMotionUpdateInterval:intervalInSeconds];
}

RCT_EXPORT_METHOD(getUpdateInterval:(RCTResponseSenderBlock) cb) {
    double interval = self->_motionManager.deviceMotionUpdateInterval;
    NSLog(@"getUpdateInterval: %f", interval);
    cb(@[[NSNull null], [NSNumber numberWithDouble:interval]]);
}

RCT_EXPORT_METHOD(getData:(RCTResponseSenderBlock) cb) {
    double accx = (self->_motionManager.deviceMotion.userAcceleration.x + self->_motionManager.deviceMotion.gravity.x) * gravitationalAcceleration;
    double accy = (self->_motionManager.deviceMotion.userAcceleration.y + self->_motionManager.deviceMotion.gravity.x) * gravitationalAcceleration;
    double accz = (self->_motionManager.deviceMotion.userAcceleration.z + self->_motionManager.deviceMotion.gravity.x) * gravitationalAcceleration;
    double gyrx = self->_motionManager.deviceMotion.rotationRate.x;
    double gyry = self->_motionManager.deviceMotion.rotationRate.y;
    double gyrz = self->_motionManager.deviceMotion.rotationRate.z;
    double timestamp = self->_motionManager.deviceMotion.timestamp;

    NSLog(@"getData: %f, %f, %f, %f, %f, %f, %f", accx, accy, accz, gyrx, gyry, gyrz, timestamp);

    cb(@[[NSNull null], @{
        @"accx" : [NSNumber numberWithDouble:accx],
        @"accy" : [NSNumber numberWithDouble:accy],
        @"accz" : [NSNumber numberWithDouble:accz],
        @"gyrx" : [NSNumber numberWithDouble:gyrx],
        @"gyry" : [NSNumber numberWithDouble:gyry],
        @"gyrz" : [NSNumber numberWithDouble:gyrz],
        @"timestamp" : [NSNumber numberWithDouble:timestamp]
        }]
    );
}

RCT_EXPORT_METHOD(startUpdates:(RCTResponseSenderBlock) errorCallback) {

    [self->_motionManager startDeviceMotionUpdates];

    /* Receive the devicemotion data on this block */
        [self->_motionManager startDeviceMotionUpdatesToQueue:[NSOperationQueue mainQueue]
            withHandler:^(CMDeviceMotion *deviceMotion, NSError *error)
    {
        double accx = (deviceMotion.userAcceleration.x + deviceMotion.gravity.x) * gravitationalAcceleration;
        double accy = (deviceMotion.userAcceleration.y + deviceMotion.gravity.y) * gravitationalAcceleration;
        double accz = (deviceMotion.userAcceleration.z + deviceMotion.gravity.z) * gravitationalAcceleration;
        double gyrx = deviceMotion.rotationRate.x;
        double gyry = deviceMotion.rotationRate.y;
        double gyrz = deviceMotion.rotationRate.z;
        double timestamp = deviceMotion.timestamp;

        [self.bridge.eventDispatcher sendDeviceEventWithName:@"DeviceMotion" body:@{
            @"accx" : [NSNumber numberWithDouble:accx],
            @"accy" : [NSNumber numberWithDouble:accy],
            @"accz" : [NSNumber numberWithDouble:accz],
            @"gyrx" : [NSNumber numberWithDouble:gyrx],
            @"gyry" : [NSNumber numberWithDouble:gyry],
            @"gyrz" : [NSNumber numberWithDouble:gyrz],
            @"timestamp" : [NSNumber numberWithDouble:timestamp]
            }];
    }];

}

RCT_EXPORT_METHOD(stopUpdates) {
    NSLog(@"stopUpdates");
    [self->_motionManager stopDeviceMotionUpdates];
}

@end
