var webpack = require("webpack");
var HtmlWebpackPlugin = require('html-webpack-plugin');
var HtmlWebpackPluginConfig = new HtmlWebpackPlugin({
    template: __dirname + '/src/index.html',
    filename: 'index.html',
    inject: 'body'
});
var FaviconsWebpackPlugin = require('favicons-webpack-plugin')

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
        new FaviconsWebpackPlugin({
            logo: './src/assets/logo.png',
            mode: 'webapp', // optional can be 'webapp' or 'light' - 'webapp' by default
            devMode: 'light', // optional can be 'webapp' or 'light' - 'light' by default
            inject: true,
            cache: true,
            prefix: '/',
            favicons: {
              appName: 'achievements',
              appDescription: 'Mina Märken',
              developerName: 'Mikael Svensson',
              developerURL: null, // prevent retrieving from the nearest package.json
              background: '#ffffff',
              theme_color: '#003C69',
              icons: {
                coast: false,
                yandex: false
              }
            }
        }),
        new webpack.DefinePlugin({
            "process.env": {
                CUSTOMER_SUPPORT_EMAIL: JSON.stringify(process.env.CUSTOMER_SUPPORT_EMAIL),
                API_HOST: JSON.stringify(process.env.API_HOST)
            }
        })
    ]
});