# FastClick  
利用javapoet生成注解类FC与快速点击工具类FastClickUtils,用户使用FC注解来标识需要进行快速点击检测的方法，编译期自动将相关FastClickUtils方法插入到对应方法中。如：  
```
   public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @FC
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the first btn");
            }
        });
        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @FC(timeInterval = 1000)
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the second btn");
            }
        });
        binding.buttonThird.setOnClickListener(new View.OnClickListener() {
            @FC(tag = "group-0")
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the third btn");
            }
        });
        binding.buttonFourth.setOnClickListener(new View.OnClickListener() {
            @FC(timeInterval = 1000, tag = "group-0")
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the fourth btn");
            }
        });
```
编译后：    
```
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.binding.buttonFirst.setOnClickListener(new OnClickListener() {
            @FC
            public void onClick(View view) {
                if (!FastClickUtils.isFastClick("", 0L)) {
                    Log.e("FirstFragment", "click the first btn");
                }
            }
        });
        this.binding.buttonSecond.setOnClickListener(new OnClickListener() {
            @FC(
                timeInterval = 1000L
            )
            public void onClick(View view) {
                if (!FastClickUtils.isFastClick("", 1000L)) {
                    Log.e("FirstFragment", "click the second btn");
                }
            }
        });
        this.binding.buttonThird.setOnClickListener(new OnClickListener() {
            @FC(
                tag = "group-0"
            )
            public void onClick(View view) {
                if (!FastClickUtils.isFastClick("group-0", 0L)) {
                    Log.e("FirstFragment", "click the third btn");
                }
            }
        });
        this.binding.buttonFourth.setOnClickListener(new OnClickListener() {
            @FC(
                timeInterval = 1000L,
                tag = "group-0"
            )
            public void onClick(View view) {
                if (!FastClickUtils.isFastClick("group-0", 1000L)) {
                    Log.e("FirstFragment", "click the fourth btn");
                }
            }
        });
    }
```
使用：  
1.gradle 配置：
详见 https://plugins.gradle.org/plugin/com.github.Buxiaohui.fastclick    
2.builde一次（以便于生成工具类FastClickUtils.java及注解类FC.java）    
3.在希望防止快速点击的地方加上注解，注解有两个参数 tag 和 intervalTime，代表分组和时间间隔，具体逻辑可以查看FastClickUtils.java  


