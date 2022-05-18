import 'package:flutter/material.dart';

import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:webview_flutter/webview_flutter.dart';

import './result.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  Future<Map<String, dynamic>> httpRequestGet(
      String urlStr, Map? headersMap) async {
    var url = Uri.parse(urlStr);
    var request = http.Request('GET', url);

    if (headersMap != null) {
      request.headers['Content-Type'] = headersMap['Content-Type'];
    }

    var streamedResponse = await request.send();

    var response = await http.Response.fromStream(streamedResponse);

    //Print the last 10 characters of the response body
    //print(response.body.substring(response.body.length - 15));
    print('Response status: ${response.statusCode}');
    print('Response body: ${response.body}');
    var temp = json.decode(response.body) as Map<String, dynamic>;

    return temp;
  }

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  TextEditingController _controller = TextEditingController();
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Search Engine"),
      ),
      body: Center(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              TextField(
                controller: _controller,
                decoration: InputDecoration(
                  hintText: "Search",
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10.0),
                  ),
                ),
              ),
              ElevatedButton(
                  onPressed: () => search(_controller.text),
                  child: const Text("Search")),
            ],
          ),
        ),
      ),
    );
  }

  void search(String query) async {
    print("Searching for $query");
    var url =
        "https://www.googleapis.com/customsearch/v1?key=AIzaSyDY0kkJiTPVd2U_W3r7X60WqMt2LghhN9g&cx=017576662512468239146:omuauf_lfve&q=$query";
    //var response = await widget.httpRequestGet(url, null);
    //print(response);

    List<String> results = ["https://en.wikipedia.org/wiki/Computer"];
    int resultsPerPage = 5;
    int noOfPages = (results.length / resultsPerPage).ceil();

    List<Result> resultList = [];
    for (int i = 0; i < noOfPages; i++) {
      resultList.add(Result(
        URL: results[i],
        searchedWord: query,
      ));
    }

    showModalBottomSheet(
        context: context,
        builder: (bCtx) {
          return Column(children: <Widget>[
            ListTile(
              title: Text("Search for $query"),
            ),
            Expanded(
              child: ListView.builder(
                itemCount: resultList.length,
                physics: const NeverScrollableScrollPhysics(),
                itemBuilder: (bCtx, index) {
                  return resultList[index];
                },
              ),
            ),
            //buttons for each page
            const Spacer(),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                for (int i = 0; i < noOfPages; i++)
                  ElevatedButton(
                    onPressed: () {},
                    child: Text("Page ${i + 1}"),
                  )
              ],
            ),
          ]);
        });
  }
}

class WebViewPage extends StatelessWidget {
  final String url;

  const WebViewPage({Key? key, required this.url}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return WebView(
      initialUrl: url,
      javascriptMode: JavascriptMode.unrestricted,
    );
  }
}
