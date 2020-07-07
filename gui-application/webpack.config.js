var webpack = require("webpack");
var HtmlWebpackPlugin = require('html-webpack-plugin');
var HtmlWebpackPluginConfig = new HtmlWebpackPlugin({
    template: __dirname + '/src/index.html',
    filename: 'index.html',
    inject: 'body'
});

module.exports = (env, argv) => ({
    entry: __dirname + '/src/index.jsx',
    module: {
        rules: [
            {
                test: /\.jsx$/,
                exclude: /node_modules/,
                use: {loader: 'babel-loader'}
            },
            {
                test: /\.handlebars$/,
                exclude: /node_modules/,
                use: {loader: 'handlebars-loader'}
            },
            {
                test: /\.(eot|woff|woff2|svg|ttf)([\?]?.*)$/,
                use: {loader: "file-loader"}
            },
            {
                test: /\.css$/,
                use: ["style-loader", "css-loader"]
            },
            {
                test: /\.s[ac]ss$/,
                use: [{
                    loader: "style-loader" // creates style nodes from JS strings
                }, {
                    loader: "css-loader" // translates CSS into CommonJS
                }, {
                    loader: "sass-loader" // compiles Sass to CSS
                }]
            }
        ]
    },
    output: {
        filename: argv.mode == 'production' ? '[name].[chunkhash].js' : '[name].[hash].js',
        chunkFilename: argv.mode == 'production' ? '[name].[chunkhash].js' : '[name].[hash].js',
        path: __dirname + '/build'
    },
    node: {
        __dirname: true,
        fs: 'empty'
    },
     optimization: {
         runtimeChunk: 'single',
         splitChunks: {
             cacheGroups: {
                 vendor: {
                     test: /[\\/]node_modules[\\/]/,
                     name: 'vendors',
                     chunks: 'all',
                     enforce: true
                 }
             }
         }
     },
    plugins: [
        HtmlWebpackPluginConfig,
        new webpack.DefinePlugin({
            "process.env": {
                CUSTOMER_SUPPORT_EMAIL: JSON.stringify(process.env.CUSTOMER_SUPPORT_EMAIL),
                API_HOST: JSON.stringify(process.env.API_HOST)
            }
        })
    ]
});