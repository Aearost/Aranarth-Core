package com.aearost.aranarthcore.utils;

import java.time.LocalDate;

/**
 * Provides utility methods to facilitate the formatting of all date related content.
 * 
 * @author Aearost
 *
 */
public class DateUtils {
	
	private int month;
	private int day;
	
	public DateUtils() {
		this.month = getMonth();
		this.day = getDay();
	}
	
	private int getMonth() {
		return LocalDate.now().getMonthValue();
	}
	
	private int getDay() {
		return LocalDate.now().getDayOfMonth();
	}
	
	public boolean isValentinesDay() {
		if (this.month == 2) {
			if (this.day >= 4 && this.day <= 14) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isEaster() {
		if (this.month == 3) {
			if (this.day >= 22 && this.day <= 31) {
				return true;
			}
		} else if (this.month == 4) {
			if (this.day >= 1 && this.day <= 25) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isHalloween() {
		if (this.month == 10) {
			if (this.day >= 20 && this.day <= 31) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isChristmas() {
		if (this.month == 12) {
			if (this.day >= 15 && this.day <= 25) {
				return true;
			}
		}
		return false;
	}
}
