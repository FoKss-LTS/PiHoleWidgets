/*
 *
 *  Copyright (C) 2022.  Reda ELFARISSI aka foxy999
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package helpers;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

public class HelperService {


    public static File createFile(String home,String file_name,String folder_name) {
        File folder = new File(home + "/" + folder_name);

        if (createFolder(home + "/" + folder_name))
            try {
                File myObj = new File(home + "/" + folder_name + "/" + file_name);
                if (myObj.createNewFile()) {
                    System.out.println("File created: " + myObj.getName());
                } else {
                    System.out.println("File already exists.");
                }
                return myObj;
            } catch (IOException e) {
                System.out.println("An error occurred: "+e.getMessage());
                e.printStackTrace();
                return null;
            }
        else {
            System.out.println("Couldn't create Folder: " + folder_name);
            return null;
        }
    }

    public static boolean createFolder(String folder_path) {
        File f1 = new File(folder_path);
        if (f1.exists())
            return true;
        return f1.mkdir();
    }

    public static String getHumanReadablePriceFromNumber(long number) {
        return NumberFormat.getIntegerInstance().format(number);
    }

    public static String convertJsonToLong(Object obj) {
        String objToReturn = (String) obj;

        return objToReturn.replaceAll(",", "");

    }

}
