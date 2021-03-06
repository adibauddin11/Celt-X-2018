package org.cafirst.frc.team5406.auto;

import org.cafirst.frc.team5406.robot.Constants;
import org.cafirst.frc.team5406.subsystems.Drive;
import org.cafirst.frc.team5406.subsystems.Intake;
import org.cafirst.frc.team5406.util.AccelFilter;
import org.cafirst.frc.team5406.util.PID;
import org.cafirst.frc.team5406.auto.MotionProfile;
import org.cafirst.frc.team5406.auto.AutoSwitchFront.AutoRunnable;

import com.ctre.phoenix.motorcontrol.ControlMode;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.Timer;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.cafirst.frc.team5406.auto.AutonomousRoutine;



public class AutoScaleSwitchRight  extends AutonomousRoutine{
	private Intake robotIntake;
	private Drive robotDrive;
	private int autoStep = 0;
	private boolean gearDelay = false;
	private double[] robotPosition;
	private int direction = 1;
    int flipWrist = 0;
    double pathComplete = 0;
    private PID turnToAnglePid = new PID();
    private AccelFilter turnAccelFilter = new AccelFilter(0.04);	
    int k = 0;
	private boolean turnToFirstRun = true;

    String gameData;
	ArrayList<Point2D> left = new ArrayList<Point2D>();

	boolean driveBackwards = false;
	MotionProfile motionProfiler = new MotionProfile();
	int wristUpDelay = 0;
	boolean drivePathDone = false;
        double startTime = 0;
        int lastPoint = 0;
        double targetTime = 0;
        int autoDelay =0;
        class PeriodicRunnable implements java.lang.Runnable {
    	    public void run() { 

    	    	int armPos;
    	    	
    	    	switch (flipWrist) {
    	    	case 1:
    	    		robotIntake._wristMotor.selectProfileSlot(0,0);
    	    		armPos = robotIntake.getArmPosition();
    	    		robotIntake._wristMotor.set(ControlMode.MotionMagic, robotIntake.wristFlipPosDown(armPos));
    	    		if(armPos < 100) {
    	    			flipWrist = 5;
    	    		}
    	    		break;
    	    	case 3:
    	    		robotIntake._wristMotor.selectProfileSlot(0,0);
    	    		armPos = robotIntake.getArmPosition();
    	    		robotIntake._wristMotor.set(ControlMode.MotionMagic, robotIntake.wristFlipPosUp(armPos));
    	    		if(armPos > Constants.ARM_UP - 100) {
    	    			flipWrist = 5;
    	    		}
    	    		break;
    	    		
    	    	}

    	    }
    	}
    	Notifier _notifier = new Notifier(new PeriodicRunnable());
        class AutoRunnable implements java.lang.Runnable {
    	private double targetAngle;
    	private double accumI = 0.0;
    	public double lastAngle = 0;
    	private double previousError = 0.0;
    	boolean droveLast = false;
    	
    	
		public void run() {

			double leftSpeed = 0;
			double rightSpeed = 0;
			double dSpeed = 0;
			double speedChangeMultiplier = 0;
			double targetSpeedLeft = 0;
			double targetSpeedRight = 0;
			double currentAngle = Constants.navX.getYaw();

			if (!drivePathDone) {

				double elapsedTime = Timer.getFPGATimestamp() - startTime;
				/*System.out.println("eTime: " + elapsedTime + "tTime: " + targetTime + "Cur:" + currentAngle + "Tar:"
						+ motionProfiler.motionProfile.get(lastPoint)[5]);*/
				int numPoints = motionProfiler.motionProfile.size();
				pathComplete = (double)lastPoint/numPoints;
				System.out.println(lastPoint + "/" + numPoints);
				if (elapsedTime > targetTime || droveLast) {
					if (lastPoint < numPoints - 1) {
						speedChangeMultiplier = calcSpeed(motionProfiler.motionProfile.get(lastPoint)[5] - currentAngle);

						if (elapsedTime > targetTime + motionProfiler.motionProfile.get(lastPoint)[0] / 1000) {
							targetSpeedLeft = 0;
							targetSpeedRight = 0;
							double profilesSkipped = 0;

							while (elapsedTime > targetTime + motionProfiler.motionProfile.get(lastPoint)[0] / 1000) {
								profilesSkipped += motionProfiler.motionProfile.get(lastPoint)[0] / 1000;
								targetTime += motionProfiler.motionProfile.get(lastPoint)[0] / 1000;

								targetSpeedLeft += (motionProfiler.motionProfile.get(lastPoint)[0] / 1000)
										* motionProfiler.motionProfile.get(lastPoint)[2] * (4096 / 600)
										* Constants.driveGearRatio;
								targetSpeedRight += (motionProfiler.motionProfile.get(lastPoint)[0] / 1000)
										* motionProfiler.motionProfile.get(lastPoint)[4] * (4096 / 600)
										* Constants.driveGearRatio;
								lastPoint++;
							}

							double leftOverTime = elapsedTime - targetTime;
							profilesSkipped += leftOverTime;

							targetSpeedLeft += leftOverTime * motionProfiler.motionProfile.get(lastPoint)[2]
									* (4096 / 600) * Constants.driveGearRatio;
							targetSpeedLeft /= profilesSkipped;
							leftSpeed = targetSpeedLeft
									+ Math.signum(targetSpeedLeft) * targetSpeedLeft * speedChangeMultiplier; // -1*400-1200
																												// =
																												// -1800
							targetSpeedRight += leftOverTime * motionProfiler.motionProfile.get(lastPoint)[4]
									* (4096 / 600) * Constants.driveGearRatio;
							targetSpeedRight /= profilesSkipped;
							rightSpeed = targetSpeedRight
									- Math.signum(targetSpeedRight) * targetSpeedRight * speedChangeMultiplier; // 400-1200
																												// =
																												// -800

							droveLast = true;

							/*System.out.println(lastPoint + " (" + (numPoints - 2) + "), "
									+ motionProfiler.motionProfile.get(lastPoint)[0] / 1000
									+ +motionProfiler.motionProfile.get(lastPoint)[2] + ", "
									+ motionProfiler.motionProfile.get(lastPoint)[1]);*/
						} else {
							droveLast = false;
							targetTime += motionProfiler.motionProfile.get(lastPoint)[0] / 1000;
							targetSpeedLeft = motionProfiler.motionProfile.get(lastPoint)[2] * (4096 / 600)
									* Constants.driveGearRatio;
							leftSpeed = targetSpeedLeft
									+ Math.signum(targetSpeedLeft) * targetSpeedLeft * speedChangeMultiplier; // -1*400-1200
																												// =
																												// -1800
							targetSpeedRight = motionProfiler.motionProfile.get(lastPoint)[4] * (4096 / 600)
									* Constants.driveGearRatio;
							rightSpeed = targetSpeedRight
									- Math.signum(targetSpeedRight) * targetSpeedRight * speedChangeMultiplier; // 400-1200
																												// =
																												// -800

							/*System.out.println("LS: " + leftSpeed + ", LT: " + targetSpeedLeft + ", LA:"
									+ robotDrive._frontLeftMotor.getSelectedSensorVelocity(0) + ", RS: " + rightSpeed
									+ ", RT: " + targetSpeedRight + ", RA:"
									+ robotDrive._frontRightMotor.getSelectedSensorVelocity(0));*/
							lastPoint++;
						}
						
						if(driveBackwards) {
							robotDrive._frontRightMotor.set(ControlMode.Velocity, -1 * leftSpeed);
							robotDrive._frontLeftMotor.set(ControlMode.Velocity, -1 * rightSpeed);
						}else {
							robotDrive._frontRightMotor.set(ControlMode.Velocity, rightSpeed);
							robotDrive._frontLeftMotor.set(ControlMode.Velocity, leftSpeed);
						}
							
							
					} else {
						robotDrive._frontLeftMotor.set(ControlMode.Velocity, 0);
						robotDrive._frontRightMotor.set(ControlMode.Velocity, 0);
						drivePathDone = true;

						System.out.println("DELTA TIME: " + elapsedTime);
					}
				}
			}

		}
    	
	    
	    public double calcSpeed(double currentError){
			
	 		double valP = Constants.GYRO_PID_P * currentError;
	 		double valI = accumI;
	 		double valD = Constants.GYRO_PID_D * (previousError - currentError);
	 		if(Math.abs(valD) > Math.abs(valP)) valD = valP; // Limit so that D isn't the driving number
	 		accumI += Constants.GYRO_PID_I;
	 		
	 		//If we overshoot, reset the I
	 		if(Math.signum(previousError) != Math.signum(currentError)){ 
	 			accumI = 0; 
	 			valI = 0;
	 		}
	 		double speed = valP + (valI * (currentError > 0 ? 1.0 : -1.0)) - valD;
	 		previousError = currentError;
	 		return speed;
	 	}
	}
	Notifier _autoLoop = new Notifier(new AutoRunnable());

	public AutoScaleSwitchRight(Drive _robotDrive, Intake _robotIntake){
		super("4 - Scale & Switch Auto Right");
		robotDrive = _robotDrive;
		robotIntake = _robotIntake;
	}
	
	public void init(){
		robotIntake.setupMotors();
		robotDrive.setupMotors();
    	robotIntake._wristMotor.set(ControlMode.MotionMagic, robotIntake.getWristPosition());

    	robotDrive._frontLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
    	robotDrive._frontRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
    	startTime = Timer.getFPGATimestamp();
         lastPoint = 0;
        targetTime = 0;
        motionProfiler.motionProfile = new ArrayList<double[]>();
 		drivePathDone = false;
         autoStep = 2;
         Constants.navX.zeroYaw();
         wristUpDelay = 0;
         autoDelay =0;
	    	robotIntake._armMotor.configMotionAcceleration(6000, Constants.kTimeoutMs);
	    	robotIntake._wristMotor.configMotionAcceleration(6000, Constants.kTimeoutMs);
	    	robotIntake._elevatorMotor.configMotionAcceleration(75000, Constants.kTimeoutMs);
			robotIntake.compressor.stop();
	    	turnToAnglePid.setConstants(Constants.GYRO_PID_P, Constants.GYRO_PID_I, Constants.GYRO_PID_D);


	}
	
	public void end(){
		_autoLoop.stop();
		robotIntake.compressor.start();
    	robotIntake._armMotor.configMotionAcceleration(12000, Constants.kTimeoutMs);
    	robotIntake._wristMotor.configMotionAcceleration(8000, Constants.kTimeoutMs);
    	robotIntake._elevatorMotor.configMotionAcceleration(100000, Constants.kTimeoutMs);

	}

	public void periodic(){
		switch(autoStep) {
	   	case 0:

			if(!robotIntake.wristZeroed) {
				robotIntake._wristMotor.configForwardSoftLimitEnable(false, Constants.kTimeoutMs);
				robotIntake._wristMotor.configReverseSoftLimitEnable(false, Constants.kTimeoutMs);
				robotIntake._wristMotor.set(0.5);
				System.out.print(robotIntake._wristMotor.getOutputCurrent());
				if(robotIntake._wristMotor.getOutputCurrent()>15) {
					robotIntake.setWristPosition(150);
					robotIntake._wristMotor.configForwardSoftLimitEnable(false, Constants.kTimeoutMs);
					robotIntake._wristMotor.configReverseSoftLimitEnable(false, Constants.kTimeoutMs);
					robotIntake.wristUp();
					robotIntake.wristZeroed = true;
				}
			}
			
			if(!robotIntake.armZeroed) {
				robotIntake._armMotor.set(-0.5);
				robotIntake._armMotor.configForwardSoftLimitEnable(false, Constants.kTimeoutMs);
				robotIntake._armMotor.configReverseSoftLimitEnable(false, Constants.kTimeoutMs);

				if(robotIntake._armMotor.getOutputCurrent()>5) {
					robotIntake._armMotor.setSelectedSensorPosition(-10, 0, Constants.kTimeoutMs);
					robotIntake._armMotor.configForwardSoftLimitEnable(false, Constants.kTimeoutMs);
					robotIntake._armMotor.configReverseSoftLimitEnable(false, Constants.kTimeoutMs);
					robotIntake.armDown();
					robotIntake.armZeroed = true;
				}
			}
			
			if(!robotIntake.elevatorZeroed) {
				robotIntake._elevatorMotor.set(-0.5);
				robotIntake._elevatorMotor.configForwardSoftLimitEnable(false, Constants.kTimeoutMs);
				robotIntake._elevatorMotor.configReverseSoftLimitEnable(false, Constants.kTimeoutMs);
				if(robotIntake._elevatorMotor.getOutputCurrent()>5) {
					robotIntake.setElevatorPosition(-500);
					robotIntake._elevatorMotor.configForwardSoftLimitEnable(true, Constants.kTimeoutMs);
					robotIntake._elevatorMotor.configReverseSoftLimitEnable(true, Constants.kTimeoutMs);
					robotIntake.elevatorDown();
					robotIntake.elevatorZeroed = true;
				}
			}
			if(robotIntake.elevatorZeroed && robotIntake.armZeroed && robotIntake.wristZeroed) {
				autoStep++;
			}
			
			
			break;
	   	case 1:
	   		autoDelay++;
	   		if(autoDelay > 1) {
	   			autoStep++;
	   		}
	   		break;
	   	case 2:
	   	   gameData = DriverStation.getInstance().getGameSpecificMessage();
           if(gameData.length() > 0)
           {
           	startTime = Timer.getFPGATimestamp();
            lastPoint = 0;
            targetTime = 0;
    		drivePathDone = false;
        	   left = new ArrayList<Point2D>();
        	   left.add(new Point2D.Double(0, 0));
        	   autoStep++;
			  if(gameData.charAt(1) == 'R'){
				  left.add(new Point2D.Double(15, 170));
				  left.add(new Point2D.Double(-33, 264));
				  motionProfiler.bezierPoints(left, 0, -15, 10, 2);
			  } else {
				  left.add(new Point2D.Double(5, 190));
				  left.add(new Point2D.Double(-165, 230));
				  left.add(new Point2D.Double(-189, 294));
				  motionProfiler.bezierPoints(left, 0, 11, 10, 2);
			  }
			  
			  
			  driveBackwards = true;
			  _autoLoop.startPeriodic(0.005);
           }
           break;
	   	case 3:
 	
 		System.out.println(pathComplete);
	   	if(pathComplete > 0.60) {

			//robotIntake.wristUp();
			/*robotIntake.armUp();
			robotIntake.elevatorFast();
			robotIntake.elevatorSwitchMid();
			flipWrist =3;
			robotIntake.wristOut = false;
			_notifier.startPeriodic(0.005);
			robotIntake.needsWristUp = false;	*/
			autoStep++;
	   		}
	   		break;
	   	case 4:
	   		//if(robotIntake.getArmPosition() > Constants.ARM_UP - 300) {
	   		//	_notifier.stop();
	   		//	robotIntake._wristMotor.set(ControlMode.MotionMagic, -2400);
	   			autoStep++;
	   		//}
	   		break;
	   	case 5:
	   		if(drivePathDone) {
	   			_autoLoop.stop();
	   			robotIntake._intakeLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	   		
	    	robotIntake._intakeRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	   		robotIntake.spinIntake(275);
	   		autoStep++;
	   		}
	   		break;
	   	case 6:
	   		//if(robotIntake._intakeLeftMotor.getSelectedSensorPosition(0) + robotIntake._intakeRightMotor.getSelectedSensorPosition(0)> 8000) {
	   			robotIntake.spinIntake(0);

	    	robotDrive._frontLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	    	robotDrive._frontRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	    	startTime = Timer.getFPGATimestamp();
	         lastPoint = 0;
	        targetTime = 0;
	        motionProfiler.motionProfile = new ArrayList<double[]>();
	 		drivePathDone = false;

		   left = new ArrayList<Point2D>();
    	   left.add(new Point2D.Double(0, 0));
				if (gameData.charAt(1) == 'R') {

					if (gameData.charAt(0) == 'R') {
						left.add(new Point2D.Double(-10, 52));
						motionProfiler.bezierPoints(left, -15, -5, 10, 1);
					} else {
						left.add(new Point2D.Double(300, 20));
						motionProfiler.bezierPoints(left, 10, 50, 10, 2);
					}
				} else {
					if (gameData.charAt(0) == 'R') {
						left.add(new Point2D.Double(-212, 25));
						motionProfiler.bezierPoints(left, 10, 50, 10, 2);
					} else {
						left.add(new Point2D.Double(-5, 20));
						left.add(new Point2D.Double(150, 20));
						motionProfiler.bezierPoints(left, 10, 0, 10, 1);
					}
				}
		  
		  driveBackwards = false;
		  _autoLoop.startPeriodic(0.005);
		  autoStep++;
		  wristUpDelay=0;
	   		//}
		  break;
		  
	   	case 7:
	   		if(drivePathDone) {
	   			_autoLoop.stop();
		    	robotDrive._frontLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
		    	robotDrive._frontRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
		    	startTime = Timer.getFPGATimestamp();
		         lastPoint = 0;
		        targetTime = 0;
		        motionProfiler.motionProfile = new ArrayList<double[]>();
		 		drivePathDone = false;
		 		wristUpDelay=0;	
		 		autoStep++;
	   		}
						  

	   		break;
	   	case 8:
	   		if (gameData.charAt(1) == 'R') {

				if (gameData.charAt(0) == 'L') {
					if(turnToAngle(0)) {
			   			autoStep++;
			   		}
				}
			} else {
				if (gameData.charAt(0) == 'R') {
					if(turnToAngle(0)) {
			   			autoStep++;
			   		}
				}
			}
	   		break;
	   		
	   case 9:
		   wristUpDelay++;
		   if(wristUpDelay > 15) {
	   			/*robotIntake.gripOpen();
	   			robotIntake.armDown();
				robotIntake.elevatorFast();
				robotIntake.elevatorDown();
				robotIntake.wristOut = true;
				flipWrist =1;
				_notifier.startPeriodic(0.005);*/
				autoStep++;
	   		}
	   		break;
	   		
	   
	   case 10:
	   		/*if(robotIntake.getArmPosition() < 200) {
	   		_notifier.stop();
	   		robotIntake.wristDownMore();
	   		robotIntake.spinIntake(-200);
			autoStep++;
	   		}*/
		   autoStep++;
	   break;
	   case 11:
	   		//if(drivePathDone) {
			//	  _autoLoop.stop();
		   		/*robotIntake._intakeLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
		    	robotIntake._intakeRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	   			robotIntake.gripFirm();*/
	   			wristUpDelay=0;
	   			autoStep++;
	   		//}
	   		break;
	   case 12:
		   wristUpDelay++;
		   if(wristUpDelay > 20) {
	   			robotIntake.spinIntake(0);

	    	robotDrive._frontLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	    	robotDrive._frontRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
	    	startTime = Timer.getFPGATimestamp();
	         lastPoint = 0;
	        targetTime = 0;
	        motionProfiler.motionProfile = new ArrayList<double[]>();
	 		drivePathDone = false;

		   left = new ArrayList<Point2D>();
  	   left.add(new Point2D.Double(0, 0));
			
		  if(gameData.charAt(0) == 'R'){
			  left.add(new Point2D.Double(0, 10));
			  motionProfiler.bezierPoints(left, 0, 10, 10, 1);
		  } else {
			  left.add(new Point2D.Double(0, 10));
			  motionProfiler.bezierPoints(left, 0, 10, 10, 1);
		  }
		  
		  driveBackwards = true;
		  _autoLoop.startPeriodic(0.005);
		   wristUpDelay=0;

		  autoStep++;
		   }
		  break;
	   case 13:
		   wristUpDelay++;
		   /*if(wristUpDelay > 10) {
	   			robotIntake.armDown();
				robotIntake.elevatorFast();
				robotIntake.elevatorDown();
				robotIntake.wristPuntMore();
				autoStep++;
				break;
		   }*/
		   autoStep++;

	   case 14:
	   		if(drivePathDone /*&& robotIntake.getWristPosition() > Constants.WRIST_PUNT_MORE - 400*/) {
	   		_autoLoop.stop();

    	robotDrive._frontLeftMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
    	robotDrive._frontRightMotor.setSelectedSensorPosition(0, 0, Constants.kTimeoutMs);
    	startTime = Timer.getFPGATimestamp();
         lastPoint = 0;
        targetTime = 0;
        motionProfiler.motionProfile = new ArrayList<double[]>();
 		drivePathDone = false;

	   left = new ArrayList<Point2D>();
	   left.add(new Point2D.Double(0, 0));
		
	  if(gameData.charAt(0) == 'R'){
		  left.add(new Point2D.Double(0, 28));
		  motionProfiler.bezierPoints(left, 0, 20, 10, 1);
	  } else {
		  left.add(new Point2D.Double(-22, 28));
		  motionProfiler.bezierPoints(left, 0, -20, 10, 1);
	  }
	  
	  driveBackwards = false;
	  _autoLoop.startPeriodic(0.005);
	   wristUpDelay=0;

	  autoStep++;
	   		}
	   break;

	   case 15:
		   if (drivePathDone) {
			   _autoLoop.stop();
	   			//robotIntake.spinIntake(500);
	   			autoStep++;
		   }
	   break;
	   case 16:
		   
		 /*  if(robotIntake._intakeLeftMotor.getSelectedSensorPosition(0) + robotIntake._intakeRightMotor.getSelectedSensorPosition(0)> 50000) {
	   			robotIntake.spinIntake(0);
	   			robotIntake.wristUp();
	   			autoStep++;
	   		}*/
	   		break;
	   	}
		
	}
	
	public boolean turnToAngle(double angle){
		if(turnToFirstRun){
			turnToAnglePid.setDesiredPosition(angle);
			turnToFirstRun = false;
			turnAccelFilter.reset();
			k = 0;
		}
		double currentAngle =  Constants.navX.getYaw(); //(navX.getYaw() + 180) % 360;
		double speed = turnToAnglePid.calcSpeed(currentAngle);
		turnAccelFilter.set(limitValue(speed, 1.0));
		if(k++ < 1 / 0.03){
			speed = turnAccelFilter.get();
		}
		speed = limitValue(applyMin(speed, 0.1), 1.0);
		
		
		robotDrive._frontLeftMotor.set(speed);
		robotDrive._frontRightMotor.set(-1*speed);

		
		return turnToAnglePid.isDone(currentAngle, 4);
	}
	
	double applyMin(double value, double min){
		if(value > -min && value < 0){
			return -min;
		}
		else if(value < min && value >0){
			return min;
		}
		else{
			return value;
		}
	}
	
	double limitValue(double value, double limit){
		if(value > limit){
			return limit;
		}
		else if(value < -limit){
			return -limit;
		}
		else{
			return value;
		}
	}
}
