package cn.whyyu.cvserver.entity;

import lombok.Data;

@Data
public class Camera {
    public String sequence;
    public String videoCode;
    public String name;
    public String lng;
    public String lat;
    public double level;
}
